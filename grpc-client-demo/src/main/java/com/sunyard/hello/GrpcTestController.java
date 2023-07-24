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

package com.sunyard.hello;

import com.sunyard.client.ManagedChannelManager;
import com.sunyard.loadbalance.LoadBalancerStrategy;
import io.grpc.Channel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GrpcTestController.
 */
@RestController
@RequestMapping("/test/grpc")
public class GrpcTestController {


    /**
     * test grpc.
     * 创建客户端channel与服务端通信,通过stub调用将信息传递到服务端的监听
     *
     * @return hello world
     */
    @GetMapping("/hello")
    public String hello() {
        /**stub可以理解为远程服务在本地的代理*/
        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(ManagedChannelManager.getGrpcClient("grpc-server-demo"));
        HelloRequest request = HelloRequest.newBuilder().setData("hello").build();
        HelloResponse response = stub.hello(request);
        return response.getData();
    }

    /**
     * channel层是对数据传输的抽象，核心的实现类是 ManagedChannel，表示逻辑上的一个channel，
     * 底层持有一个物理的transport（TCP通道,参见NettyClientTransport）
     * 使用ManagedChannelBuilder来创建客户端channel,
     * 默认的实现时NettyChannelBuilder
     */
    private Channel channel() {
        ManagedChannelManager.initGrpcClient("grpc-server-demo", LoadBalancerStrategy.ROUND_ROBIN.getStrategy());
        return ManagedChannelManager.getGrpcClient("grpc-server-demo");
    }

}
