package com.mdframe.forge.flow.client.event;

import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowComplete;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves the existing annotations to unique synchronous callbacks in the default local transaction. */
public class FlowCallbackCatalog {
    private final ApplicationContext context;
    private final AnnotationTransactionAttributeSource transactions = new AnnotationTransactionAttributeSource();

    public FlowCallbackCatalog(ApplicationContext context) {
        this.context = context;
    }

    public List<FlowCallbackInvocation> resolve(FlowEventContext event) {
        Map<String, FlowCallbackInvocation> callbacks = new LinkedHashMap<>();
        for (String name : context.getBeanDefinitionNames()) {
            if (isApplicationBean(name)) {
                collect(name, event, callbacks);
            }
        }
        return new ArrayList<>(callbacks.values());
    }

    private boolean isApplicationBean(String name) {
        if (context instanceof ConfigurableApplicationContext configurable) {
            var factory = configurable.getBeanFactory();
            return !factory.containsBeanDefinition(name)
                    || factory.getBeanDefinition(name).getRole() == BeanDefinition.ROLE_APPLICATION;
        }
        return true;
    }

    private void collect(String name, FlowEventContext event, Map<String, FlowCallbackInvocation> callbacks) {
        Class<?> type = context.getType(name);
        if (type == null) {
            return;
        }
        Object bean = context.getBean(name);
        Class<?> target = AopUtils.getTargetClass(bean);
        FlowBind binding = AnnotatedElementUtils.findMergedAnnotation(target, FlowBind.class);
        if (binding == null || !("*".equals(binding.modelKey())
                || binding.modelKey().equals(event.getProcessDefKey()))) {
            return;
        }
        FlowComplete complete = AnnotatedElementUtils.findMergedAnnotation(target, FlowComplete.class);
        String completeMethod = completeMethod(complete, event.getEvent());
        boolean completeResolved = completeMethod.isBlank();
        for (Method method : target.getMethods()) {
            FlowCallback annotation = AnnotatedElementUtils.findMergedAnnotation(method, FlowCallback.class);
            boolean annotated = annotation != null && Arrays.asList(annotation.on()).contains(event.getEvent());
            boolean named = method.getName().equals(completeMethod);
            if (annotated || named) {
                validateLocalMethod(method, target);
                AopUtils.selectInvocableMethod(method, bean.getClass());
                String identity = name + ":" + target.getName() + ":" + method.toGenericString();
                callbacks.putIfAbsent(identity, new FlowCallbackInvocation(identity, bean, method));
                completeResolved |= named;
            }
        }
        if (!completeResolved) {
            throw new IllegalStateException("FLOW_EVENT_CALLBACK_NOT_FOUND");
        }
    }

    private void validateLocalMethod(Method method, Class<?> target) {
        boolean signature = method.getReturnType() == Void.TYPE && (method.getParameterCount() == 0
                || method.getParameterCount() == 1
                && method.getParameterTypes()[0].isAssignableFrom(FlowEventContext.class));
        if (!signature || AnnotatedElementUtils.hasAnnotation(method, Async.class)
                || AnnotatedElementUtils.hasAnnotation(target, Async.class)) {
            throw new IllegalStateException("FLOW_EVENT_CALLBACK_MUST_BE_SYNCHRONOUS");
        }
        TransactionAttribute attribute = transactions.getTransactionAttribute(method, target);
        if (attribute != null && (attribute.isReadOnly() || !joinsLocalTransaction(attribute)
                || attribute.getQualifier() != null && !attribute.getQualifier().isBlank())) {
            throw new IllegalStateException("FLOW_EVENT_CALLBACK_MUST_JOIN_LOCAL_TRANSACTION");
        }
    }

    private boolean joinsLocalTransaction(TransactionAttribute attribute) {
        int propagation = attribute.getPropagationBehavior();
        return propagation == TransactionDefinition.PROPAGATION_REQUIRED
                || propagation == TransactionDefinition.PROPAGATION_SUPPORTS
                || propagation == TransactionDefinition.PROPAGATION_MANDATORY;
    }

    private String completeMethod(FlowComplete complete, String event) {
        if (complete == null) {
            return "";
        }
        return switch (event) {
            case FlowCallback.ON_COMPLETED -> complete.onApproved();
            case FlowCallback.ON_REJECTED -> complete.onRejected();
            case FlowCallback.ON_CANCELED -> complete.onCanceled();
            default -> "";
        };
    }
}
