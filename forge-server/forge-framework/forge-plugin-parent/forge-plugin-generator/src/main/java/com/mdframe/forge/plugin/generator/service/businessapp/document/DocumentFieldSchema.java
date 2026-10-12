package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Document field validation and deterministic number-field inference. */
public final class DocumentFieldSchema {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time"
    );

    private record DocumentNoCandidate(String fieldName, int score, int index) {
    }

    private final AiCrudConfigMapper crudConfigMapper;
    private final ObjectMapper objectMapper;
    private final DocumentConfigJson json;

    public DocumentFieldSchema(AiCrudConfigMapper crudConfigMapper, ObjectMapper objectMapper, DocumentConfigJson json) {
        this.crudConfigMapper = crudConfigMapper;
        this.objectMapper = objectMapper;
        this.json = json;
    }

    public String resolveRuntimeDocumentNoField(AiBusinessDocumentConfig config, LowcodeRuntimeConfig runtimeConfig) {
        if (config == null) {
            return null;
        }
        Map<String, Object> options = json.readObjectMap(config.getOptions());
        String configured = StringUtils.firstNonBlank(
                text(options.get("documentNoField")),
                text(options.get("documentNoFieldCode")),
                text(options.get("noField")));
        if (StringUtils.isNotBlank(configured)) {
            return configured.trim();
        }
        return inferRuntimeDocumentNoField(runtimeConfig);
    }

    public String inferRuntimeDocumentNoField(LowcodeRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return null;
        }
        List<LowcodeFieldSchema> fields = inferenceFields(runtimeConfig);
        DocumentNoCandidate best = null;
        for (int index = 0; index < fields.size(); index++) {
            LowcodeFieldSchema field = fields.get(index);
            best = chooseDocumentNoCandidate(best, field.getField(), field.getColumnName(), field.getLabel(), index);
        }
        return best == null || best.score() < 75 ? null : best.fieldName();
    }

    private List<LowcodeFieldSchema> inferenceFields(LowcodeRuntimeConfig config) {
        List<LowcodeFieldSchema> fields = new java.util.ArrayList<>();
        LowcodeModelSchema model = readInferenceModel(config.getModelSchema());
        if (model != null && model.getFields() != null) {
            model.getFields().stream().filter(java.util.Objects::nonNull).forEach(fields::add);
        }
        for (Map<String, Object> input : readInferenceEdit(config.getEditSchema())) {
            if (input == null) {
                continue;
            }
            LowcodeFieldSchema field = new LowcodeFieldSchema();
            field.setField(StringUtils.firstNonBlank(text(input.get("field")), text(input.get("prop")),
                    text(input.get("key")), text(input.get("model"))));
            field.setColumnName(StringUtils.firstNonBlank(text(input.get("columnName")), text(input.get("fieldCode"))));
            field.setLabel(StringUtils.firstNonBlank(text(input.get("label")), text(input.get("title"))));
            fields.add(field);
        }
        return fields;
    }

    private LowcodeModelSchema readInferenceModel(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        try {
            return objectMapper.readValue(source, LowcodeModelSchema.class);
        } catch (JsonProcessingException incompatibleSchema) {
            return null;
        }
    }

    private List<Map<String, Object>> readInferenceEdit(String source) {
        if (StringUtils.isBlank(source)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> fields = objectMapper.readValue(source, new TypeReference<>() {});
            return fields == null ? List.of() : fields;
        } catch (JsonProcessingException incompatibleSchema) {
            return List.of();
        }
    }

    private DocumentNoCandidate chooseDocumentNoCandidate(DocumentNoCandidate current,
                                                          String fieldName,
                                                          String columnName,
                                                          String label,
                                                          int index) {
        String resolvedField = StringUtils.firstNonBlank(fieldName, columnName);
        if (StringUtils.isBlank(resolvedField)) {
            return current;
        }
        int score = scoreDocumentNoCandidate(fieldName, columnName, label);
        if (score <= 0) {
            return current;
        }
        DocumentNoCandidate next = new DocumentNoCandidate(resolvedField.trim(), score, index);
        if (current == null || next.score() > current.score()
                || (next.score() == current.score() && next.index() < current.index())) {
            return next;
        }
        return current;
    }

    private int scoreDocumentNoCandidate(String fieldName, String columnName, String label) {
        int score = Math.max(scoreDocumentNoCode(fieldName), scoreDocumentNoCode(columnName));
        String labelText = StringUtils.defaultString(label);
        if (StringUtils.containsAny(labelText, "申请单号", "单据编号", "单据号", "业务单号")) {
            score = Math.max(score, 120);
        } else if (StringUtils.containsAny(labelText, "单号", "编号", "流水号")) {
            score = Math.max(score, 90);
        }
        return score;
    }

    private int scoreDocumentNoCode(String code) {
        String normalized = normalizeFieldKey(code);
        if (StringUtils.isBlank(normalized)) {
            return 0;
        }
        return switch (normalized) {
            case "documentno", "documentnumber" -> 120;
            case "applicationno", "applicationnumber", "applyno", "applynumber" -> 115;
            case "billno", "billnumber", "orderno", "ordernumber", "businessno", "businessnumber" -> 105;
            case "serialno", "serialnumber" -> 95;
            default -> normalized.endsWith("no") || normalized.endsWith("number") ? 60 : 0;
        };
    }

    private String normalizeFieldKey(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    public void validateRequiredField(String label, String field) {
        if (StringUtils.isBlank(field)) {
            throw new BusinessException(label + UiMessages.translate("不能为空"));
        }
    }

    public void validateOptionalObjectField(AiBusinessObject object, String field, String label, Long tenantId) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        validateObjectField(object, field, label, tenantId);
    }

    public void validateObjectField(AiBusinessObject object, String field, String label, Long tenantId) {
        Set<String> fields = collectObjectFields(object, tenantId);
        if (!fields.contains(field)) {
            throw new BusinessException(label + UiMessages.translate("不存在: ") + field);
        }
    }

    private Set<String> collectObjectFields(AiBusinessObject object, Long tenantId) {
        Set<String> fields = new LinkedHashSet<>(SYSTEM_FIELDS);
        if (object == null || StringUtils.isBlank(object.getConfigKey())) {
            return fields;
        }
        LowcodeRuntimeConfig config = RuntimeCrudConfigAdapter.fromDesign(crudConfigMapper.selectByConfigKey(tenantId, object.getConfigKey()));
        if (config == null || StringUtils.isBlank(config.getModelSchema())) {
            return fields;
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            if (modelSchema.getFields() == null) {
                return fields;
            }
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                addFieldAlias(fields, field.getField());
                addFieldAlias(fields, field.getColumnName());
            }
        } catch (Exception e) {
            throw new BusinessException(UiMessages.translate("读取业务对象字段失败: ") + e.getMessage());
        }
        return fields;
    }

    private void addFieldAlias(Set<String> fields, String field) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        fields.add(field);
        fields.add(DocumentFieldNames.snakeToCamel(field));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public String resolveDocumentNoField(AiBusinessDocumentConfig config, AiCrudConfig runtimeConfig) {
        return resolveRuntimeDocumentNoField(config, RuntimeCrudConfigAdapter.fromDesign(runtimeConfig));
    }

    public String inferDocumentNoField(AiCrudConfig runtimeConfig) {
        return inferRuntimeDocumentNoField(RuntimeCrudConfigAdapter.fromDesign(runtimeConfig));
    }
}
