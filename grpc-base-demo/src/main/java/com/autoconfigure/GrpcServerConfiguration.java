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

package com.autoconfigure;

import com.sunyard.server.GrpcClientEventListener;
import com.sunyard.server.GrpcServerBuilder;
import com.sunyard.server.GrpcServerRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Grpc type client bean postprocessor.
 */
@Configuration
@ConditionalOnProperty(value = "grpc.server.runner.enabled", havingValue = "true")
public class GrpcServerConfiguration {

    /**
     * Grpc client event listener.
     *
     * @return the grpc client bean post processor
     */
    @Bean
    public GrpcClientEventListener grpcClientEventListener() {
        return new GrpcClientEventListener();
    }

    /**
     * Grpc Server.
     *
     * @param grpcServerBuilder       grpcServerBuilder
     * @param grpcClientEventListener grpcClientEventListener
     * @return the grpc server
     */
    @Bean
    public GrpcServerRunner grpcServer(@Autowired(required = false) final GrpcServerBuilder grpcServerBuilder,
                                       final GrpcClientEventListener grpcClientEventListener) {
        return new GrpcServerRunner(grpcServerBuilder, grpcClientEventListener);
    }
}
