package com.sunyard.client;

import com.google.common.collect.Maps;
import com.sunyard.loadbalance.LoadBalancerStrategy;
import io.grpc.ManagedChannel;

import java.util.Map;
import java.util.Objects;

/**
 * @author 微服务底座平台
 * @version 2.0.0
 * @title: ManagedChannelManager
 * @projectName: grpc-parent
 * @description: ManagedChannel管理
 * @date: 2023-07-20 14:53
 **/
public class ManagedChannelManager {
    // key为ServiceName
    private static final Map<String, ManagedChannel> CLIENT_CACHE = Maps.newConcurrentMap();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Map.Entry<String, ManagedChannel> entry : CLIENT_CACHE.entrySet()) {
                ManagedChannel grpcClient = entry.getValue();
                grpcClient.shutdown();
            }
            CLIENT_CACHE.clear();
        }));
    }

    private ManagedChannelManager() {
    }

    /**
     * 创建channel.
     *
     * @param contextPath contextPath grpcServerName
     * @param loadBalance {@link LoadBalancerStrategy#getStrategy()}
     */
    public static void initGrpcClient(final String contextPath, String loadBalance) {
        CLIENT_CACHE.computeIfAbsent(contextPath, s -> GrpcClientBuilder.buildClientChannel(contextPath, loadBalance));
    }

    /**
     * 获取客户端channel.
     *
     * @param contextPath contextPath
     * @return GrpcClient GrpcClient
     */
    public static ManagedChannel getGrpcClient(final String contextPath) {
        ManagedChannel managedChannel = CLIENT_CACHE.get(contextPath);
        // 获取时如果channel连接为空时,将创建客户端channel
        if (managedChannel == null){
            CLIENT_CACHE.computeIfAbsent(contextPath, s -> GrpcClientBuilder.buildClientChannel(contextPath, LoadBalancerStrategy.ROUND_ROBIN.getStrategy()));
        }
        return CLIENT_CACHE.get(contextPath);
    }

    /**
     * 删除客户端channel.
     *
     * @param contextPath contextPath
     */
    public static void removeClient(final String contextPath) {
        ManagedChannel grpcClient = CLIENT_CACHE.remove(contextPath);
        // 关闭grpc连接ManagedChannel
        if (Objects.nonNull(grpcClient)) {
            grpcClient.shutdown();
        }
    }
}
