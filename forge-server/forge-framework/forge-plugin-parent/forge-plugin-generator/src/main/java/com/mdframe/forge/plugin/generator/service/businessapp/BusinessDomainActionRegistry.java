package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 领域动作注册中心。
 */
@Component
public class BusinessDomainActionRegistry {

    private final Map<String, BusinessDomainActionExecutor> executorMap;

    public BusinessDomainActionRegistry(List<BusinessDomainActionExecutor> executors) {
        Map<String, BusinessDomainActionExecutor> registry = new LinkedHashMap<>();
        if (executors != null) {
            for (BusinessDomainActionExecutor executor : executors) {
                String actionType = normalizeActionType(executor.actionType());
                if (StringUtils.isBlank(actionType)) {
                    throw new IllegalStateException(UiMessages.translate("领域动作执行器 actionType 不能为空: ") + executor.getClass().getName());
                }
                if (registry.containsKey(actionType)) {
                    throw new IllegalStateException(UiMessages.translate("重复注册领域动作: ") + actionType);
                }
                registry.put(actionType, executor);
            }
        }
        this.executorMap = Collections.unmodifiableMap(registry);
    }

    public BusinessDomainActionExecutor require(String actionType) {
        String normalized = normalizeActionType(actionType);
        if (StringUtils.isBlank(normalized)) {
            throw new BusinessException("领域动作类型不能为空");
        }
        BusinessDomainActionExecutor executor = executorMap.get(normalized);
        if (executor == null) {
            throw new BusinessException(UiMessages.translate("未注册领域动作: ") + normalized);
        }
        return executor;
    }

    public boolean supports(String actionType) {
        String normalized = normalizeActionType(actionType);
        return StringUtils.isNotBlank(normalized) && executorMap.containsKey(normalized);
    }

    public Map<String, BusinessDomainActionExecutor> registeredExecutors() {
        return executorMap;
    }

    static String normalizeActionType(String actionType) {
        return StringUtils.defaultString(actionType)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace("-", "_")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
