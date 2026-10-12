package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionStepDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionStepResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 领域动作步骤执行器。
 */
@Component
@RequiredArgsConstructor
public class DomainActionStepExecutor implements BusinessActionStepExecutor {

    private final BusinessDomainActionRegistry domainActionRegistry;

    @Override
    public String supportType() {
        return "DOMAIN_ACTION";
    }

    @Override
    public BusinessActionStepResultVO execute(BusinessActionExecutionContext context, BusinessActionStepDTO step) {
        Map<String, Object> config = step.getStepConfig() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(step.getStepConfig());
        String actionType = StringUtils.firstNonBlank(
                BusinessActionStepConfigHelper.firstText(config, "domainActionType"),
                BusinessActionStepConfigHelper.firstText(config, "actionType"),
                BusinessActionStepConfigHelper.firstText(config, "domainType"),
                BusinessActionStepConfigHelper.firstText(config, "type"));
        if (StringUtils.isBlank(actionType)) {
            throw new BusinessException("领域动作步骤缺少 actionType");
        }

        Map<String, Object> resolvedConfig = resolveConfig(config, context);
        resolvedConfig.put("actionType", BusinessDomainActionRegistry.normalizeActionType(actionType));
        resolvedConfig.put("stepCode", step.getStepCode());
        resolvedConfig.put("stepName", step.getStepName());
        BusinessDomainActionExecutor executor = domainActionRegistry.require(actionType);
        BusinessActionStepResultVO result = executor.execute(context, resolvedConfig);
        if (result != null) {
            if (result.getResult() == null) {
                result.setResult(new LinkedHashMap<>());
            }
            result.getResult().putIfAbsent("domainActionType", resolvedConfig.get("actionType"));
        }
        return result;
    }

    private Map<String, Object> resolveConfig(Map<String, Object> config, BusinessActionExecutionContext context) {
        Map<String, Object> resolved = new LinkedHashMap<>(config);
        Map<String, Object> params = BusinessActionStepConfigHelper.asMap(
                BusinessActionStepConfigHelper.firstValue(config, "params", "parameters", "actionConfig", "operationConfig"));
        if (!params.isEmpty()) {
            resolved.put("params", resolveParams(params, context));
        }
        Map<String, Object> mappedData = BusinessActionStepConfigHelper.buildData(config, context);
        if (!mappedData.isEmpty()) {
            Map<String, Object> mergedParams = new LinkedHashMap<>(
                    BusinessActionStepConfigHelper.asMap(resolved.get("params")));
            mergedParams.putAll(mappedData);
            resolved.put("params", mergedParams);
        }
        return resolved;
    }

    private Map<String, Object> resolveParams(Map<String, Object> params, BusinessActionExecutionContext context) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        params.forEach((key, value) -> resolved.put(key, resolveValue(value, context)));
        return resolved;
    }

    private Object resolveValue(Object value, BusinessActionExecutionContext context) {
        if (!(value instanceof String text)) {
            return value;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}") && trimmed.length() > 3) {
            return BusinessActionStepConfigHelper.resolvePath(trimmed.substring(2, trimmed.length() - 1), context);
        }
        return value;
    }
}
