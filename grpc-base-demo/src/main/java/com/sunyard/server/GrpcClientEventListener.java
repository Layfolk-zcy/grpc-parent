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

import com.google.common.collect.Lists;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * The type Shenyu grpc client event listener.
 * @author Lenovo
 */
public class GrpcClientEventListener extends AbstractContextRefreshedEventListener<BindableService, GrpcServerImpl> {

    private final List<ServerServiceDefinition> serviceDefinitions = Lists.newArrayList();

    /**
     * Instantiates a new Shenyu client bean post processor.
     */
    public GrpcClientEventListener() {
        super();
    }

    @Override
    protected void handle(final String beanName, final BindableService bean) {
        exportGenericService(bean);
        super.handle(beanName, bean);
    }

    @Override
    protected Map<String, BindableService> getBeans(final ApplicationContext context) {
        return context.getBeansOfType(BindableService.class);
    }


    @Override
    protected Class<GrpcServerImpl> getAnnotationType() {
        return GrpcServerImpl.class;
    }

    private void exportGenericService(final BindableService bindableService) {
        ServerServiceDefinition serviceDefinition = bindableService.bindService();
        try {
            serviceDefinitions.add(serviceDefinition);
        } catch (Exception e) {
            LOG.error("export json generic service is fail", e);
        }
    }

    /**
     * get serviceDefinitions.
     *
     * @return serviceDefinitions
     */
    public List<ServerServiceDefinition> getServiceDefinitions() {
        return serviceDefinitions;
    }
}
