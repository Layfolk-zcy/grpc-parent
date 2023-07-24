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

package com.sunyard.client;

import com.sunyard.intercept.ContextClientInterceptor;
import com.sunyard.loadbalance.LoadBalancerStrategy;
import com.sunyard.loadbalance.RandomLoadBalancerProvider;
import com.sunyard.loadbalance.RoundRobinLoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Grpc client 创建builder.
 */
@Component
public final class GrpcClientBuilder {

    private static final String DISCOVERY_SCHEMA = "discovery:///";


    static {
        // 将自定义的负载策略注入到grpc的负载注册器中供后面grpc负载调用时查找对应的负载提供器
        LoadBalancerRegistry.getDefaultRegistry().register(new RandomLoadBalancerProvider());
        LoadBalancerRegistry.getDefaultRegistry().register(new RoundRobinLoadBalancerProvider());
        // 通过NameResolverRegistry方式注册NameResolver
        //NameResolverRegistry.getDefaultRegistry().register(new DiscoveryClientResolverProvider(client));
    }

    private GrpcClientBuilder() {
    }

    /**
     * Build the client channel.
     *
     * @return ManagedChannel
     */
    public static ManagedChannel buildClientChannel(String contextPath, String loadBalance) {

        if (StringUtil.isNullOrEmpty(loadBalance)) {
            loadBalance = LoadBalancerStrategy.RANDOM.getStrategy();
        } else {
            // 策略方式不是自定义中某一种
            List<String> strategyNames = Arrays.stream(LoadBalancerStrategy.values()).map(item -> item.getStrategy()).collect(Collectors.toList());
            // 全部转为小写方式比较策略名称
            if (!strategyNames.contains(loadBalance.toLowerCase(Locale.ROOT))) {
                loadBalance = LoadBalancerStrategy.RANDOM.getStrategy();
            }
        }

        if (!contextPath.contains(DISCOVERY_SCHEMA)) {
            contextPath = "discovery:///" + contextPath;
        }
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                // build channel to server with server's address "discovery:///serverName"
                .forTarget(contextPath)
                // 设置拦截器
                .intercept(new ContextClientInterceptor())
                // 设置默认的负载规则
                .defaultLoadBalancingPolicy(loadBalance)
                // 不会再去尝试升级http1
                .usePlaintext()
                // 消息传输大小限制
                .maxInboundMessageSize(100 * 1024 * 1024)
                // 关闭重试
                .disableRetry();
        ManagedChannel channel = builder.build();
        channel.getState(true);
        return channel;
    }

}
