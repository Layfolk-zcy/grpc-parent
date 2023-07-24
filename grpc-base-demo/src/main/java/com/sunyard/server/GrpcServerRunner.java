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

package com.sunyard.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;
import java.util.List;

/**
 * Add grpc service and start grpc server.
 */
public class GrpcServerRunner implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final GrpcServerBuilder grpcServerBuilder;
    /**通过该监听获取所有GrpcClient注解修改的类通过获取的类信息转换为ServerServiceDefinition*/
    private final GrpcClientEventListener grpcClientEventListener;

    public GrpcServerRunner(final GrpcServerBuilder grpcServerBuilder,
                            final GrpcClientEventListener grpcClientEventListener) {
        this.grpcServerBuilder = grpcServerBuilder;
        this.grpcClientEventListener = grpcClientEventListener;
    }
    
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        startGrpcServer();
    }
    /**实现GrpcServerBuilder接口的grpc server方式启动该服务  */
    private void startGrpcServer() {
        // 通过grpcServerBuilder获取到对应的服务端定义信息即绑定的端口
        ServerBuilder<?> serverBuilder = grpcServerBuilder.buildServerBuilder();
        // 获取所有服务端需要注册的方法信息
        List<ServerServiceDefinition> serviceDefinitions = grpcClientEventListener.getServiceDefinitions();
        for (ServerServiceDefinition serviceDefinition : serviceDefinitions) {
            serverBuilder.addService(serviceDefinition);
            LOG.info("{} has been add to grpc server", serviceDefinition.getServiceDescriptor().getName());
        }
        // 服务端启动
        try {
            Server server = serverBuilder.build().start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("shutting down grpc server");
                server.shutdown();
                LOG.info("grpc server shut down");
            }));

            LOG.info("Grpc server started successfully");
        } catch (IOException e) {
            LOG.error("Grpc server failed to start", e);
        }
    }
}
