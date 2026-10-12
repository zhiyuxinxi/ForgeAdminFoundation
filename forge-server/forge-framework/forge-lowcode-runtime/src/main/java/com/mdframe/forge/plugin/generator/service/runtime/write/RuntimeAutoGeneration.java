package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.api.code.BusinessCodeGenerator;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumbers;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import com.mdframe.forge.starter.core.exception.BusinessException;

/** Applies generated values; transaction and data-source lifetime belong to the caller. */
@lombok.extern.slf4j.Slf4j
@lombok.RequiredArgsConstructor
public final class RuntimeAutoGeneration {
    private final RuntimeGenerationRules rules;
    private final RuntimeWriteFieldMapping fieldMapping;
    private final BusinessCodeGenerator codeRuleService;
    private final RuntimeDocumentNumbers documentNumbers;

    public void apply(LowcodeRuntimeConfig config, Map<String, Object> data, Set<String> allowedFields) {
        if (config == null || data == null) {
            return;
        }
        List<RuntimeGenerationField> autoFields = rules.explicit(config);
        if (autoFields.isEmpty()) {
            applyConventionalCodeRuleFields(config, data, allowedFields);
            return;
        }
        Map<String, Object> mainPayload = data;
        for (RuntimeGenerationField field : autoFields) {
            if (!field.enabled() || !"ON_CREATE".equalsIgnoreCase(field.trigger())) {
                continue;
            }
            Set<String> writableAliases = fieldMapping.generatedAliases(config,
                    StringUtils.firstNonBlank(field.fieldName(), field.columnName()));
            if (writableAliases.isEmpty()) {
                log.warn("[DynamicCrudService] 自动编号字段未匹配到业务表列, configKey={}, field={}",
                        config.getConfigKey(), field.fieldName());
                continue;
            }
            if (allowedFields != null) {
                allowedFields.addAll(writableAliases);
            }
            if ("EMPTY_ONLY".equalsIgnoreCase(field.fillPolicy()) && hasFieldValue(mainPayload, writableAliases)) {
                continue;
            }
            String generatedCode = codeRuleService.generate(field.ruleCode(), buildCodeRuleContext(config, mainPayload, field));
            if (StringUtils.isBlank(generatedCode)) {
                continue;
            }
            for (String alias : writableAliases) {
                mainPayload.put(alias, generatedCode);
            }
        }
        applyConventionalCodeRuleFields(config, data, allowedFields);
    }

    private void applyConventionalCodeRuleFields(LowcodeRuntimeConfig config, Map<String, Object> data, Set<String> allowedFields) {
        List<RuntimeGenerationField> fields = rules.conventional(config);
        if (fields.isEmpty()) {
            return;
        }
        Map<String, Object> mainPayload = data;
        for (RuntimeGenerationField field : fields) {
            Set<String> writableAliases = fieldMapping.generatedAliases(config,
                    StringUtils.firstNonBlank(field.fieldName(), field.columnName()));
            if (writableAliases.isEmpty() || hasFieldValue(mainPayload, writableAliases)) {
                continue;
            }
            String generatedCode = tryGenerateConventionalCode(config, mainPayload, field);
            if (StringUtils.isBlank(generatedCode)) {
                continue;
            }
            if (allowedFields != null) {
                allowedFields.addAll(writableAliases);
            }
            for (String alias : writableAliases) {
                mainPayload.put(alias, generatedCode);
            }
        }
    }

    private String tryGenerateConventionalCode(LowcodeRuntimeConfig config,
                                               Map<String, Object> mainPayload,
                                               RuntimeGenerationField field) {
        try {
            return codeRuleService.generate(field.ruleCode(), buildCodeRuleContext(config, mainPayload, field));
        } catch (BusinessException e) {
            log.debug("[DynamicCrudService] 未使用约定编码规则生成字段, configKey={}, field={}, ruleCode={}, reason={}",
                    config.getConfigKey(), field.fieldName(), field.ruleCode(), e.getMessage());
            return null;
        }
    }

    private boolean hasFieldValue(Map<String, Object> data, Set<String> aliases) {
        if (data == null || aliases == null || aliases.isEmpty()) {
            return false;
        }
        for (String alias : aliases) {
            Object value = data.get(alias);
            if (value != null && (!(value instanceof String textValue) || StringUtils.isNotBlank(textValue))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildCodeRuleContext(LowcodeRuntimeConfig config,
                                                     Map<String, Object> recordData,
                                                     RuntimeGenerationField field) {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> fields = new LinkedHashMap<>();
        if (recordData != null) {
            fields.putAll(recordData);
        }
        fields.put("suiteCode", StringUtils.defaultIfBlank(config.getDomainCode(), ""));
        fields.put("objectCode", StringUtils.defaultIfBlank(config.getObjectCode(), ""));
        fields.put("configKey", StringUtils.defaultIfBlank(config.getConfigKey(), ""));
        fields.put("fieldCode", StringUtils.defaultIfBlank(field.fieldName(), field.columnName()));
        fields.put("fieldName", StringUtils.defaultIfBlank(field.label(), field.fieldName()));
        context.put("fields", fields);
        return context;
    }

    /** Applies the document-number fallback after configured and conventional generation. */
    public void applyDocumentNumber(LowcodeRuntimeConfig config, Map<String, Object> mainPayload, Set<String> allowedFields) {
        if (config == null || mainPayload == null) {
            return;
        }
        RuntimeDocumentNumber documentConfig = documentNumbers.resolve(config);
        if (documentConfig == null) {
            return;
        }
        String documentNoField = documentConfig.documentNoField();
        if (StringUtils.isBlank(documentNoField)) {
            return;
        }
        Set<String> writableAliases = fieldMapping.generatedAliases(config, documentNoField);
        if (writableAliases.isEmpty()) {
            log.warn("[DynamicCrudService] 单据编号字段未匹配到业务表列, configKey={}, documentNoField={}",
                    config.getConfigKey(), documentNoField);
            return;
        }
        if (allowedFields != null) {
            allowedFields.addAll(writableAliases);
        }
        if (isConfigured(config, documentNoField)) {
            return;
        }
        String documentNo = documentNumbers.generate(documentConfig, mainPayload);
        if (StringUtils.isBlank(documentNo)) {
            return;
        }
        // 单据编号由平台规则统一生成，覆盖客户端传入值，避免用户手填造成冲突。
        for (String alias : writableAliases) {
            mainPayload.put(alias, documentNo);
        }
    }

    public boolean isConfigured(LowcodeRuntimeConfig config, String fieldName) {
        return rules.isConfigured(config, fieldName);
    }
}
