package com.mdframe.forge.flow.client.event;

import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Invokes the Spring bean itself, including JDK/CGLIB advice; never unwraps its target. */
public record FlowCallbackInvocation(String identity, Object bean, Method method) {
    public void invoke(FlowEventContext context) {
        Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
        try {
            ReflectionUtils.makeAccessible(invocable);
            if (invocable.getParameterCount() == 0) {
                invocable.invoke(bean);
            } else {
                invocable.invoke(bean, context);
            }
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("FLOW_EVENT_CALLBACK_FAILED", cause);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("FLOW_EVENT_CALLBACK_NOT_INVOCABLE", exception);
        }
    }
}
