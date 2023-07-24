/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sunyard.loadbalance;

import com.sunyard.loadbalance.picker.AbstractPicker;
import com.sunyard.loadbalance.picker.AbstractReadyPicker;
import com.sunyard.loadbalance.picker.EmptyPicker;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.*;

/**
 * LoadBalancer.
 * gRPC 中提供了 round_robin, pick_first, grpclb, HealthCheckingRoundRobin 等负载均衡的实现，
 * 默认使用HealthCheckingRoundRobin，该负载均衡支持检查 Subchannel 的健康状态
 * <p>
 * 该类实现了地址的处理，根据地址创建 Subchannel，并启动 Subchannel 状态监听器
 * <p>
 * gRPC的负载均衡是基于每次调用而不是每条连接的.
 * 换句话说,即使所有的请求来自同一个客户端,我们也希望这些请求能够在不同的服务上进行LB
 */
public abstract class AbstractLoadBalancer extends LoadBalancer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLoadBalancer.class);

    private static final Status EMPTY_OK = Status.OK.withDescription("no subchannels ready");
    // 提供LoadBalancer实现的基本要素,用于进行SubChannel的创建、更新辅助
    private final Helper helper;

    private final AtomicReference<String> serviceName = new AtomicReference<>();
    // 缓存创建完成的SubChannel
    public static final Map<EquivalentAddressGroup, Subchannel> subchannels = new ConcurrentHashMap<>();
    // grpc连接状态
    private ConnectivityState currentState;

    private AbstractPicker currentPicker = new EmptyPicker(EMPTY_OK);

    protected AbstractLoadBalancer(final Helper helper) {
        this.helper = checkNotNull(helper, "helper");
    }
    // 设置当前对应负载的服务名,便于日志打印
    private String getServiceName() {
        return serviceName.get();
    }

    //
    private void setAttribute(final Attributes attributes) {
        this.serviceName.compareAndSet(null, attributes.get(GrpcAttributeUtils.APP_NAME));
    }

    // grpc直接使用时直接使用handler中的ip:port创建grpc链接并存储Channel,strippedAddressGroup-->ip:port
    // 为服务列表中的每个服务建立一个subchannel
    @Override
    public void handleResolvedAddresses(final ResolvedAddresses resolvedAddresses) {
        // 设置当前进行解析的服务名
        setAttribute(resolvedAddresses.getAttributes());
        // 从本地内存中获取
        Set<EquivalentAddressGroup> currentAddrs = subchannels.keySet();
        // key对象中只封装了getAddresses()方法获取的值
        Map<EquivalentAddressGroup, EquivalentAddressGroup> latestAddrs = stripAttrs(resolvedAddresses.getAddresses());
        // 获取将本地缓存中的多余数据
        Set<EquivalentAddressGroup> removedAddrs = setsDifference(currentAddrs, latestAddrs.keySet());
        // 处理nameResolver传递过来的数据
        for (Map.Entry<EquivalentAddressGroup, EquivalentAddressGroup> latestEntry : latestAddrs.entrySet()) {
            EquivalentAddressGroup strippedAddressGroup = latestEntry.getKey();
            EquivalentAddressGroup originalAddressGroup = latestEntry.getValue();
            Subchannel subchannel;
            // 获取本地已创建好连接的SubChannel
            Subchannel existingSubchannel = subchannels.get(strippedAddressGroup);
            if (Objects.nonNull(existingSubchannel)) {
                subchannel = existingSubchannel;
                // 更新当前channel中关联的attribute属性
                SubChannels.updateAttributes(existingSubchannel, originalAddressGroup.getAttributes());
            } else {
                // 重新创建
                subchannel = SubChannels.createSubChannel(helper, strippedAddressGroup, originalAddressGroup.getAttributes());
                //SubchannelStateListener是Subchannel 的状态监听器，当 Subchannel 状态发生变化时进行处理
                subchannel.start(state -> processSubchannelState(subchannel, state));
                subchannels.put(strippedAddressGroup, subchannel);
            }
            // 建立连接
            subchannel.requestConnection();
        }
        List<Subchannel> removedSubchannels = new ArrayList<>();
        for (EquivalentAddressGroup addressGroup : removedAddrs) {
            removedSubchannels.add(subchannels.remove(addressGroup));
        }
        updateBalancingState();
        // 关闭本地缓存中被移除的subChannel
        for (Subchannel removedSubchannel : removedSubchannels) {
            shutdownSubchannel(removedSubchannel);
        }
    }

    private void processSubchannelState(final Subchannel subchannel, final ConnectivityStateInfo stateInfo) {
        if (subchannels.get(stripAttrs(subchannel.getAddresses())) != subchannel) {
            return;
        }
        if (stateInfo.getState() == IDLE) {
            subchannel.requestConnection();
            LOG.info("AbstractLoadBalancer.handleSubchannelState, current state:IDLE, subchannel.requestConnection().");
        }
        final ConnectivityStateInfo originStateInfo = SubChannels.getStateInfo(subchannel);
        if (originStateInfo.getState().equals(TRANSIENT_FAILURE) && (stateInfo.getState().equals(CONNECTING) || stateInfo.getState().equals(IDLE))) {
            return;
        }
        SubChannels.setStateInfo(subchannel, stateInfo);
        updateBalancingState();
    }

    private Map<EquivalentAddressGroup, EquivalentAddressGroup> stripAttrs(final List<EquivalentAddressGroup> groupList) {
        Map<EquivalentAddressGroup, EquivalentAddressGroup> addrs = new HashMap<>(groupList.size() * 2);
        for (EquivalentAddressGroup group : groupList) {
            addrs.put(stripAttrs(group), group);
        }
        return addrs;
    }

    private static EquivalentAddressGroup stripAttrs(final EquivalentAddressGroup eag) {
        return new EquivalentAddressGroup(eag.getAddresses());
    }

    private <T> Set<T> setsDifference(final Set<T> a, final Set<T> b) {
        Set<T> aCopy = new HashSet<>(a);
        aCopy.removeAll(b);
        return aCopy;
    }

    @Override
    public void shutdown() {
        for (Subchannel subchannel : subchannels.values()) {
            shutdownSubchannel(subchannel);
        }
    }

    private void shutdownSubchannel(final Subchannel subchannel) {
        subchannel.shutdown();
        SubChannels.setStateInfo(subchannel, ConnectivityStateInfo.forNonError(SHUTDOWN));
    }

    @Override
    public void handleNameResolutionError(final Status error) {
        updateBalancingState(TRANSIENT_FAILURE,
                currentPicker instanceof AbstractReadyPicker ? currentPicker : new EmptyPicker(error));
    }

    /**
     * Updates picker with the list of active subchannels (state == READY).
     */
    private void updateBalancingState() {
        final List<Subchannel> activeList = subchannels.values()
                .stream()
                .filter(r -> SubChannels.getStateInfo(r).getState() == READY)
                .collect(Collectors.toList());
        if (activeList.isEmpty()) {
            // No READY subchannels
            boolean isConnecting = false;
            Status aggStatus = EMPTY_OK;
            for (Subchannel subchannel : getSubchannels()) {
                ConnectivityStateInfo stateInfo = SubChannels.getStateInfo(subchannel);
                if (stateInfo.getState() == CONNECTING || stateInfo.getState() == IDLE) {
                    isConnecting = true;
                }
                if (aggStatus == EMPTY_OK || !aggStatus.isOk()) {
                    aggStatus = stateInfo.getStatus();
                }
            }
            // 针对subChannel状态为CONNECTING或TRANSIENT_FAILURE不使用负载策略
            updateBalancingState(isConnecting ? CONNECTING : TRANSIENT_FAILURE, new EmptyPicker(aggStatus));
        } else {
            updateBalancingState(READY, newPicker(new ArrayList<>(subchannels.values())));
        }
    }

    private void updateBalancingState(final ConnectivityState state, final AbstractPicker picker) {
        if (state == currentState && picker.isEquivalentTo(currentPicker)) {
            return;
        }
        helper.updateBalancingState(state, picker);
        currentState = state;
        currentPicker = picker;
        LOG.info("AbstractPicker update, serviceName:{}, all subchannels:{}, state:{}", serviceName, picker.getSubchannelsInfo(), state);
    }

    private Collection<Subchannel> getSubchannels() {
        return subchannels.values();
    }

    /**
     * Create new picker.
     *
     * @param list all subchannels
     * @return ReadyPicker
     */
    protected abstract AbstractReadyPicker newPicker(List<Subchannel> list);
}
