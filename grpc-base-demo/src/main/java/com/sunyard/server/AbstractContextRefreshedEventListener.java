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

import cn.hutool.core.collection.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type abstract context refreshed event listener.
 *
 * @author Lenovo
 */
public abstract class AbstractContextRefreshedEventListener<T, A extends Annotation> implements ApplicationListener<ContextRefreshedEvent> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractContextRefreshedEventListener.class);

    /**
     * api path separator.
     */
    protected static final String PATH_SEPARATOR = "/";

    private final AtomicBoolean registered = new AtomicBoolean(false);


    /**
     * Instantiates a new context refreshed event listener.
     */
    public AbstractContextRefreshedEventListener() {
    }

    @Override
    public void onApplicationEvent(@NonNull final ContextRefreshedEvent event) {
        final ApplicationContext context = event.getApplicationContext();
        Map<String, T> beans = getBeans(context);
        if (CollectionUtil.isEmpty(beans)) {
            return;
        }
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        beans.forEach(this::handle);
    }


    protected abstract Map<String, T> getBeans(ApplicationContext context);

    protected void handle(final String beanName, final T bean) {
    }

    protected Class<?> getCorrectedClass(final T bean) {
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        return clazz;
    }

    protected abstract Class<A> getAnnotationType();

}
