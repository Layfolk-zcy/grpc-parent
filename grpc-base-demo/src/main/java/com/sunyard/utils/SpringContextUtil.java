package com.sunyard.utils;

import org.springframework.context.ApplicationContext;

/**
 * @author 微服务底座平台
 * @version 2.0.0
 * @title: SpringContextUtil
 * @projectName: grpc-parent
 * @description: spring 上下文工具类
 * @date: 2023-07-23 22:03
 **/
public class SpringContextUtil {
    private static ApplicationContext applicationContext;
    /**
     * Description 获取上下文
     * @date 2023/7/24 10:03
     * @param:
     * @return: org.springframework.context.ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    /**
     * Description 设置上下文
     * @date 2023/7/24 10:03
     * @param: applicationContext
     * @return: void
     */
    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtil.applicationContext = applicationContext;
    }
    /***
     * Description 通过名字获取上下文中的bean
     * @date 2023/7/24 10:02
     * @param: name
     * @return: java.lang.Object
     */
    public static Object getBean(String name){
        return applicationContext.getBean(name);
    }
    /**
     * Description 通过类型获取上下文中的bean
     * @date 2023/7/24 10:03
     * @param: requiredType
     * @return: java.lang.Object
     */
    public static Object getBean(Class<?> requiredType){
        return applicationContext.getBean(requiredType);
    }
}

