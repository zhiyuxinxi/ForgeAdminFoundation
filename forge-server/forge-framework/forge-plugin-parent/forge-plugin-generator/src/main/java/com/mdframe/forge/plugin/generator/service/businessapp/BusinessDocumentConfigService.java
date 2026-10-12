package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentNoRulePreviewDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRuleTokenVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentConfigJson;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentStatusPolicy;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentFieldSchema;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentNumberTemplate;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentNumberSequence;
import com.mdframe.forge.plugin.generator.service.businessapp.document.DocumentFlowBinding;
import org.springframework.stereotype.Service;

/** Owns document configuration persistence and transaction orchestration. */
@Service
public class BusinessDocumentConfigService {

    private final BusinessDocumentConfigMapper documentConfigMapper;
    private final BusinessObjectService objectService;
    private final AiCrudConfigMapper crudConfigMapper;
    private final DocumentConfigJson json;
    private final DocumentStatusPolicy statuses;
    private final DocumentFieldSchema fieldSchema;
    private final DocumentNumberTemplate templates;
    private final DocumentNumberSequence numbers;
    private final DocumentFlowBinding flowBindings;

    public BusinessDocumentConfigService(BusinessDocumentConfigMapper documentConfigMapper,
            BusinessBindingMapper bindingMapper, BusinessObjectService objectService,
            AiCrudConfigMapper crudConfigMapper, ObjectMapper objectMapper, ISequenceService sequenceService) {
        this.documentConfigMapper = documentConfigMapper;
        this.objectService = objectService;
        this.crudConfigMapper = crudConfigMapper;
        this.json = new DocumentConfigJson(objectMapper);
        this.statuses = new DocumentStatusPolicy(objectMapper);
        this.fieldSchema = new DocumentFieldSchema(crudConfigMapper, objectMapper, json);
        this.templates = new DocumentNumberTemplate();
        this.numbers = new DocumentNumberSequence(sequenceService, templates);
        this.flowBindings = new DocumentFlowBinding(bindingMapper, objectMapper, json);
    }

    public BusinessDocumentConfigVO getConfig(Long objectId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(resolveTenantId(), objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(resolveTenantId(), object.getObjectCode());
        }
        if (config == null) {
            BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
            vo.setObjectId(object.getId());
            vo.setSuiteCode(object.getSuiteCode());
            vo.setObjectCode(object.getObjectCode());
            vo.setConfigKey(object.getConfigKey());
            vo.setDocumentEnabled(false);
            vo.setDocumentName(object.getObjectName() + "单据");
            vo.setStatusMapping(statuses.defaultStatusMapping());
            vo.setStatusMappingRows(statuses.defaultStatusRows());
            vo.setMainFlowSummary(flowBindings.buildMainFlowSummary(resolveTenantId(), object.getObjectCode(), null));
            return vo;
        }
        return toVO(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Long objectId, BusinessDocumentConfigDTO dto) {
        if (dto == null) {
            throw new BusinessException("单据配置不能为空");
        }
        AiBusinessObject object = objectService.requireEntity(objectId);
        boolean enabled = Boolean.TRUE.equals(dto.getDocumentEnabled());
        String documentNoRule = templates.normalizeNoRuleTemplate(StringUtils.firstNonBlank(dto.getNoRuleTemplate(), dto.getDocumentNoRule()));
        List<BusinessDocumentConfigVO.StatusMappingRowVO> statusRows = statuses.normalizeStatusRows(
                dto.getStatusMappingRows(), dto.getStatusMapping());
        Map<String, String> statusMapping = statuses.statusMappingFromRows(statusRows);
        if (StringUtils.isNotBlank(documentNoRule)) {
            templates.validateNoRuleTemplate(documentNoRule);
        }
        Long tenantId = resolveTenantId();
        String documentNoField = text(json.readObjectMap(dto.getOptions()).get("documentNoField"));
        if (enabled && StringUtils.isNotBlank(documentNoRule) && StringUtils.isBlank(documentNoField)) {
            AiCrudConfig runtimeConfig = crudConfigMapper.selectByConfigKey(tenantId, object.getConfigKey());
            documentNoField = fieldSchema.inferDocumentNoField(runtimeConfig);
        }
        if (enabled) {
            fieldSchema.validateRequiredField("单据状态字段", dto.getStatusField());
            fieldSchema.validateObjectField(object, dto.getStatusField(), "单据状态字段", tenantId);
            fieldSchema.validateOptionalObjectField(object, dto.getStarterField(), "发起人字段", tenantId);
            fieldSchema.validateOptionalObjectField(object, dto.getOwnerField(), "负责人字段", tenantId);
            if (StringUtils.isNotBlank(documentNoRule) && StringUtils.isBlank(documentNoField)) {
                throw new BusinessException("启用编号规则时必须配置单据编号字段");
            }
            fieldSchema.validateOptionalObjectField(object, documentNoField, "单据编号字段", tenantId);
        }

        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(tenantId, objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(tenantId, object.getObjectCode());
        }
        if (config == null) {
            config = new AiBusinessDocumentConfig();
            config.setTenantId(tenantId);
            config.setObjectId(object.getId());
            config.setSuiteCode(object.getSuiteCode());
            config.setObjectCode(object.getObjectCode());
        }
        config.setObjectId(object.getId());
        config.setSuiteCode(object.getSuiteCode());
        config.setObjectCode(object.getObjectCode());
        config.setConfigKey(object.getConfigKey());
        config.setDocumentEnabled(enabled ? 1 : 0);
        config.setDocumentName(StringUtils.defaultIfBlank(dto.getDocumentName(), object.getObjectName() + "单据"));
        config.setDocumentNoRule(StringUtils.trimToNull(documentNoRule));
        config.setStatusField(StringUtils.trimToNull(dto.getStatusField()));
        config.setStarterField(StringUtils.trimToNull(dto.getStarterField()));
        config.setOwnerField(StringUtils.trimToNull(dto.getOwnerField()));
        String requestedFlowKey = StringUtils.trimToNull(dto.getDefaultFlowKey());
        AiBusinessBinding mainFlowBinding = flowBindings.selectMainFlowBinding(resolveTenantId(), object.getObjectCode());
        config.setDefaultFlowKey(mainFlowBinding == null
                ? requestedFlowKey
                : StringUtils.defaultIfBlank(flowBindings.resolveFlowModelKey(flowBindings.readBindingConfig(mainFlowBinding.getBindingConfig())),
                        mainFlowBinding.getBindingKey()));
        config.setStatusMapping(json.writeJson(statusMapping, "单据状态映射"));
        config.setOptions(json.writeJson(buildOptions(dto, documentNoRule, documentNoField, statusRows), "单据扩展配置"));

        if (config.getId() == null) {
            documentConfigMapper.insert(config);
        } else {
            documentConfigMapper.updateById(config);
        }
        flowBindings.syncLegacyFlowBindingIfNeeded(object, requestedFlowKey, config.getDocumentName(), tenantId);
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(String objectCode) {
        return selectEnabledByObjectCode(resolveTenantId(), objectCode);
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
        if (config == null || !Integer.valueOf(1).equals(config.getDocumentEnabled())) {
            return null;
        }
        return config;
    }

    public AiBusinessDocumentConfig selectEnabledByConfigKey(Long tenantId, String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return null;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByConfigKey(
                tenantId != null ? tenantId : resolveTenantId(), configKey);
        if (config == null || !Integer.valueOf(1).equals(config.getDocumentEnabled())) {
            return null;
        }
        return config;
    }

    public BusinessDocumentConfigVO toVO(AiBusinessDocumentConfig config) {
        return toVO(config, null);
    }

    public BusinessDocumentConfigVO toVO(AiBusinessDocumentConfig config, AiCrudConfig runtimeConfig) {
        Map<String, Object> options = json.readObjectMap(config.getOptions());
        Long tenantId = config.getTenantId() != null ? config.getTenantId() : resolveTenantId();
        Map<String, Object> mainFlowSummary = flowBindings.buildMainFlowSummary(tenantId, config.getObjectCode(), config.getDefaultFlowKey());
        BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
        vo.setId(config.getId());
        vo.setObjectId(config.getObjectId());
        vo.setSuiteCode(config.getSuiteCode());
        vo.setObjectCode(config.getObjectCode());
        vo.setConfigKey(config.getConfigKey());
        vo.setDocumentEnabled(Integer.valueOf(1).equals(config.getDocumentEnabled()));
        vo.setDocumentName(config.getDocumentName());
        String normalizedDocumentNoRule = templates.normalizeNoRuleTemplate(config.getDocumentNoRule());
        vo.setDocumentNoRule(normalizedDocumentNoRule);
        vo.setNoRuleTemplate(StringUtils.defaultIfBlank(templates.normalizeNoRuleTemplate(text(options.get("noRuleTemplate"))), normalizedDocumentNoRule));
        if (StringUtils.isNotBlank(vo.getNoRuleTemplate())) {
            BusinessDocumentNoRulePreviewDTO previewDTO = new BusinessDocumentNoRulePreviewDTO();
            previewDTO.setTemplate(vo.getNoRuleTemplate());
            previewDTO.setSuiteCode(config.getSuiteCode());
            previewDTO.setObjectCode(config.getObjectCode());
            vo.setNoRulePreview(previewNoRule(previewDTO));
        }
        vo.setStatusField(config.getStatusField());
        vo.setStarterField(config.getStarterField());
        vo.setOwnerField(config.getOwnerField());
        vo.setDefaultFlowKey(StringUtils.defaultIfBlank(text(mainFlowSummary.get("flowModelKey")), config.getDefaultFlowKey()));
        vo.setStatusMapping(statuses.readStringMap(config.getStatusMapping()));
        vo.setStatusMappingRows(readStatusRows(options, vo.getStatusMapping()));
        vo.setStatusActionPolicy(json.readObjectMap(options.get("statusActionPolicy")));
        vo.setMainFlowSummary(mainFlowSummary);
        String documentNoField = text(options.get("documentNoField"));
        if (StringUtils.isBlank(documentNoField)) {
            documentNoField = resolveDocumentNoField(config,
                    runtimeConfig != null ? runtimeConfig : selectRuntimeConfig(tenantId, config));
        }
        if (StringUtils.isNotBlank(documentNoField)) {
            options.put("documentNoField", documentNoField);
        }
        vo.setOptions(options);
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private AiCrudConfig selectRuntimeConfig(Long tenantId, AiBusinessDocumentConfig config) {
        if (config == null) {
            return null;
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiCrudConfig runtimeConfig = null;
        if (StringUtils.isNotBlank(config.getConfigKey())) {
            runtimeConfig = crudConfigMapper.selectByConfigKey(effectiveTenantId, config.getConfigKey());
        }
        if (runtimeConfig == null && StringUtils.isNotBlank(config.getObjectCode())) {
            runtimeConfig = crudConfigMapper.selectPublishedByObjectCode(effectiveTenantId, config.getObjectCode());
        }
        return runtimeConfig;
    }

    public void syncDefaultFlowKeyByObjectCode(Long tenantId, String objectCode, String flowModelKey) {
        if (StringUtils.isBlank(objectCode)) {
            return;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
        if (config == null) {
            return;
        }
        config.setDefaultFlowKey(StringUtils.trimToNull(flowModelKey));
        documentConfigMapper.updateById(config);
    }

    private Map<String, Object> buildOptions(BusinessDocumentConfigDTO dto,
                                             String documentNoRule,
                                             String documentNoField,
                                             List<BusinessDocumentConfigVO.StatusMappingRowVO> statusRows) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (dto.getOptions() != null) {
            options.putAll(dto.getOptions());
        }
        options.put("noRuleTemplate", StringUtils.trimToNull(documentNoRule));
        options.put("documentNoField", StringUtils.trimToNull(documentNoField));
        options.put("statusMappingRows", statusRows);
        options.put("statusActionPolicy", dto.getStatusActionPolicy() == null
                ? new LinkedHashMap<>()
                : dto.getStatusActionPolicy());
        return options;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public List<BusinessDocumentNoRuleTokenVO> listNoRuleTokens() {
        return templates.listNoRuleTokens();
    }

    public BusinessDocumentNoRulePreviewVO previewNoRule(BusinessDocumentNoRulePreviewDTO dto) {
        return templates.previewNoRule(dto);
    }

    public String resolveDocumentNoField(AiBusinessDocumentConfig config, AiCrudConfig runtimeConfig) {
        return fieldSchema.resolveDocumentNoField(config, runtimeConfig);
    }

    public String resolveRuntimeDocumentNoField(AiBusinessDocumentConfig config, LowcodeRuntimeConfig runtimeConfig) {
        return fieldSchema.resolveRuntimeDocumentNoField(config, runtimeConfig);
    }

    public String generateDocumentNo(AiBusinessDocumentConfig config, Map<String, Object> recordData) {
        if (config == null || !Integer.valueOf(1).equals(config.getDocumentEnabled())
                || StringUtils.isBlank(config.getDocumentNoRule())) {
            return null;
        }
        Long tenantId = config.getTenantId() != null ? config.getTenantId() : resolveTenantId();
        return numbers.generateDocumentNo(config, recordData, tenantId);
    }

    public String generateRuntimeDocumentNo(RuntimeDocumentNumber config, Map<String, Object> recordData) {
        if (config == null || !Integer.valueOf(1).equals(config.documentEnabled())
                || StringUtils.isBlank(config.rawNoRule())) {
            return null;
        }
        Long tenantId = config.tenantId() != null ? config.tenantId() : resolveTenantId();
        return numbers.generateRuntimeDocumentNo(config, recordData, tenantId);
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> readStatusRows(
            Map<String, Object> options, Map<String, String> legacyMapping) {
        return statuses.readStatusRows(options, legacyMapping);
    }

}
