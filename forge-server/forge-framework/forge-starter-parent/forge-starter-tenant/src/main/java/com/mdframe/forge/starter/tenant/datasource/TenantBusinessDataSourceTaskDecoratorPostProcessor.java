package com.mdframe.forge.starter.tenant.datasource;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 为 Spring 管理的线程池统一设置租户业务数据源上下文装饰器。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.business.datasource", name = "enabled", havingValue = "true")
public class TenantBusinessDataSourceTaskDecoratorPostProcessor implements BeanPostProcessor {

    private final TenantBusinessDataSourceTaskDecorator taskDecorator;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ThreadPoolTaskExecutor executor) {
            executor.setTaskDecorator(taskDecorator);
        }
        return bean;
    }
}
