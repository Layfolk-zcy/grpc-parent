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

package com.sunyard.intercept;

import io.grpc.*;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;
import java.util.Optional;

/**
 * Grpc context interceptor.
 * grpc客户端拦截器,从发送的请求头中获取到需要传递到grpc服务端
 */
public class ContextClientInterceptor implements ClientInterceptor {

    public static final Context.Key<Map<String, String>> RPC_CONTEXT_KEY = Context.key("grpcContext");

    @Override
    public <R, P> ClientCall<R, P> interceptCall(final MethodDescriptor<R, P> methodDescriptor, final CallOptions callOptions, final Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<R, P>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(final Listener<P> responseListener, final Metadata headers) {
                Optional.ofNullable(RPC_CONTEXT_KEY.get()).ifPresent(map -> map.forEach((k, v) -> {
                    headers.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v);
                }));
                // 将Context中的内容通过Metadata传递到请求头后将当前Context解除
                Context.current().detach(Context.ROOT);
                super.start(responseListener, headers);
            }
        };
    }

    public static void grpcContextPopulate(final ServerWebExchange exchange){
        Map<String, Map<String, String>> rpcContext = exchange.getAttribute("generalContext");
        Optional.ofNullable(rpcContext).map(context -> context.get("grpc")).ifPresent(
                context -> Context.current().withValue(RPC_CONTEXT_KEY, context).attach());
    }
}
