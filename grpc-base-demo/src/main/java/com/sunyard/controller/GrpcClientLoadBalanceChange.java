package com.sunyard.controller;

import com.sunyard.client.ManagedChannelManager;
import com.sunyard.loadbalance.resolver.DiscoveryClientResolverProvider;
import com.sunyard.utils.SpringContextUtil;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author 微服务底座平台
 * @version 2.0.0
 * @title: GrpcClientLoadBalanceChange
 * @projectName: grpc-parent
 * @description: grpc client负载均衡策略修改
 * @date: 2023-07-20 15:27
 **/
@RestController
@RequestMapping("/grpc")
@ConditionalOnProperty(value = "grpc.client.api.enabled", havingValue = "true")
public class GrpcClientLoadBalanceChange {

    @Autowired
    DiscoveryClient client;
    @Autowired
    private ApplicationContext applicationContext;

    @PostMapping("/loadbalance/strategy")
    public void grpcClientLoadBalance(String loadBalance, String grpcServerName) {
        ManagedChannel grpcClient = ManagedChannelManager.getGrpcClient(grpcServerName);
        if (Objects.nonNull(grpcClient)) {
            //removeBean("grpcDiscoveryClientResolverFactory");
            //addBean("grpcDiscoveryClientResolverFactory", DiscoveryClientResolverProvider.class);
            ManagedChannelManager.removeClient(grpcServerName);
            ManagedChannelManager.initGrpcClient(grpcServerName, loadBalance);
        }
    }

    private void addBean(String beanName, Class<?> beanClass) {
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {

            //将applicationContext转换为ConfigurableApplicationContext
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) SpringContextUtil.getApplicationContext();

            // 获取bean工厂并转换为DefaultListableBeanFactory
            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();

            // 通过BeanDefinitionBuilder创建bean定义
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
            ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
            constructorArgumentValues.addGenericArgumentValue(client);
            // 注册bean
            defaultListableBeanFactory.registerBeanDefinition("grpcDiscoveryClientResolverFactory", beanDefinitionBuilder.getRawBeanDefinition());
        }
    }

    private void removeBean(String beanName) {
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
        beanDefinitionRegistry.getBeanDefinition(beanName);
        beanDefinitionRegistry.removeBeanDefinition(beanName);
    }
}
