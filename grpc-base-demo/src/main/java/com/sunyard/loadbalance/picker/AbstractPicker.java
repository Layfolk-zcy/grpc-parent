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

package com.sunyard.loadbalance.picker;

import io.grpc.LoadBalancer;

/**
 * Picker abstract.抽象类 子类实现自定义的负载策略，Subchannel 选择器，根据不同的策略使用不同的选择方式
 */
public abstract class AbstractPicker extends LoadBalancer.SubchannelPicker {

    /**
     * The target picker is equivalent to this.
     *
     * @param picker target picker
     * @return picker is equivalent
     */
    public abstract boolean isEquivalentTo(AbstractPicker picker);

    /**
     * Get the target subChannels.
     *
     * @return subChannels infos
     */
    public abstract String getSubchannelsInfo();
}

