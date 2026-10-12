package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves tenant-scoped runtime object identities, configurations and flow bindings in legacy precedence order. */
@RequiredArgsConstructor
final class BusinessFlowRuntimeMetadata {
    private final BusinessBindingMapper bindingMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;

    void ensureBusinessBinding(JSONObject config, Long tenantId, String objectCode) {
        if (config == null) {
            return;
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, objectCode, runtimeConfig);
        JSONObject defaults = FlowBindingSchema.normalizeBusinessBinding(FlowBindingSchema.defaultBusinessBinding(runtimeConfig, documentConfig));
        JSONObject current = config.getJSONObject("businessBinding");
        if (current == null || current.isEmpty()) {
            if (!defaults.isEmpty()) {
                config.put("businessBinding", defaults);
            }
            return;
        }
        FlowBindingSchema.mergeBusinessBindingDefaults(current, defaults);
        config.put("businessBinding", current);
    }

    static Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    static Long resolveActiveOrgId() {
        try {
            return SessionHelper.getActiveOrgId();
        } catch (Exception e) {
            return null;
        }
    }

    static String resolveUsername() {
        try {
            var loginUser = SessionHelper.getLoginUser();
            if (loginUser != null) {
                return StringUtils.firstNonBlank(loginUser.getRealName(), loginUser.getUsername());
            }
            return SessionHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        return objects == null || objects.isEmpty() ? null : objects.get(0);
    }

    AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectPublishedByObjectCodeOrConfigKey(
                tenantId != null ? tenantId : resolveTenantId(), objectCodeOrConfigKey);
    }

    AiCrudConfig resolveRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectRuntimeByObjectCodeOrConfigKey(
                tenantId != null ? tenantId : resolveTenantId(), objectCodeOrConfigKey);
    }

    AiBusinessBinding selectMainFlowBindingForConfig(Long tenantId, String objectCode, String... fallbackCodes) {
        return selectMainFlowBinding(tenantId, objectCode, fallbackCodes).binding();
    }

    BindingLookupResult selectMainFlowBinding(Long tenantId, String objectCode, String... fallbackCodes) {
        List<String> candidates = objectCodeCandidates(objectCode, fallbackCodes);
        AiBusinessBinding firstDisabled = null;
        String disabledObjectCode = null;
        for (String candidate : candidates) {
            AiBusinessBinding binding = bindingMapper.selectBindingByTypeAndCode(tenantId, "OBJECT", candidate, "FLOW");
            if (isBindingEnabled(binding)) {
                return new BindingLookupResult(binding, candidate, candidates);
            }
            if (firstDisabled == null && binding != null) {
                firstDisabled = binding;
                disabledObjectCode = candidate;
            }
            AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                    tenantId, "OBJECT", candidate, "APPROVAL");
            if (isBindingEnabled(legacyApprovalBinding)) {
                return new BindingLookupResult(legacyApprovalBinding, candidate, candidates);
            }
            if (firstDisabled == null && legacyApprovalBinding != null) {
                firstDisabled = legacyApprovalBinding;
                disabledObjectCode = candidate;
            }
        }
        return new BindingLookupResult(firstDisabled, disabledObjectCode, candidates);
    }

    List<String> objectCodeCandidates(String objectCode, String... fallbackCodes) {
        Set<String> candidates = new LinkedHashSet<>();
        addObjectCodeCandidate(candidates, objectCode);
        if (fallbackCodes != null) {
            for (String fallbackCode : fallbackCodes) {
                addObjectCodeCandidate(candidates, fallbackCode);
            }
        }
        return new ArrayList<>(candidates);
    }

    void addObjectCodeCandidate(Set<String> candidates, String objectCode) {
        String normalized = StringUtils.trimToNull(objectCode);
        if (StringUtils.isNotBlank(normalized)) {
            candidates.add(normalized);
        }
    }

    String resolveCanonicalObjectCode(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return objectCodeOrConfigKey;
        }
        BusinessRuntimeContext context = resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey);
        return StringUtils.firstNonBlank(context.objectCode(), StringUtils.trimToNull(objectCodeOrConfigKey));
    }

    BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey) {
        return resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey, false);
    }

    BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey,
                                                                 boolean allowDraftRuntime) {
        String requestedObjectCode = StringUtils.trimToNull(objectCodeOrConfigKey);
        if (requestedObjectCode == null) {
            return new BusinessRuntimeContext(null, null, null, null, null, null);
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, requestedObjectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, requestedObjectCode, runtimeConfig);
        AiBusinessObject businessObject = resolveBusinessObject(tenantId, requestedObjectCode, runtimeConfig, documentConfig);
        String canonicalObjectCode = canonicalObjectCode(documentConfig, businessObject, runtimeConfig, requestedObjectCode);

        if (documentConfig == null && !StringUtils.equals(canonicalObjectCode, requestedObjectCode)) {
            documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode, runtimeConfig);
        }
        if (runtimeConfig == null) {
            runtimeConfig = resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    documentConfig == null ? null : documentConfig.getConfigKey(),
                    businessObject == null ? null : businessObject.getConfigKey(),
                    canonicalObjectCode));
        }
        if (runtimeConfig == null && allowDraftRuntime) {
            runtimeConfig = resolveRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    documentConfig == null ? null : documentConfig.getConfigKey(),
                    businessObject == null ? null : businessObject.getConfigKey(),
                    canonicalObjectCode,
                    requestedObjectCode));
        }
        if (businessObject == null && !StringUtils.equals(canonicalObjectCode, requestedObjectCode)) {
            businessObject = resolveBusinessObject(tenantId, canonicalObjectCode, runtimeConfig, documentConfig);
        }
        String configKey = StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getConfigKey(),
                runtimeConfig == null ? null : runtimeConfig.getConfigKey(),
                businessObject == null ? null : businessObject.getConfigKey());
        return new BusinessRuntimeContext(
                requestedObjectCode,
                canonicalObjectCode,
                configKey,
                documentConfig,
                runtimeConfig,
                businessObject);
    }

    private String canonicalObjectCode(AiBusinessDocumentConfig documentConfig, AiBusinessObject businessObject,
                                       AiCrudConfig runtimeConfig, String requestedObjectCode) {
        return StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getObjectCode(),
                businessObject == null ? null : businessObject.getObjectCode(),
                runtimeConfig == null ? null : runtimeConfig.getObjectCode(),
                requestedObjectCode);
    }

    AiBusinessDocumentConfig resolveEnabledDocumentConfig(Long tenantId, String objectCodeOrConfigKey,
                                                                  AiCrudConfig runtimeConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessDocumentConfig config = documentConfigService.selectEnabledByObjectCode(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null || runtimeConfig == null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        if (config != null) {
            return config;
        }
        return documentConfigService.selectEnabledByObjectCode(effectiveTenantId, runtimeConfig.getObjectCode());
    }

    AiBusinessObject resolveBusinessObject(Long tenantId, String objectCodeOrConfigKey,
                                                   AiCrudConfig runtimeConfig,
                                                   AiBusinessDocumentConfig documentConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessObject object = null;
        if (documentConfig != null && StringUtils.isNotBlank(documentConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, documentConfig.getConfigKey());
        }
        if (object == null && runtimeConfig != null && StringUtils.isNotBlank(runtimeConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        }
        if (object == null && StringUtils.isNotBlank(objectCodeOrConfigKey)) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        }
        if (object == null && StringUtils.isNotBlank(objectCodeOrConfigKey)) {
            object = businessObjectMapper.selectFirstByObjectCode(effectiveTenantId, objectCodeOrConfigKey);
        }
        return object;
    }

    static Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return tenantId != null ? tenantId : 1L;
    }

    static boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !Integer.valueOf(0).equals(binding.getStatus());
    }

    static JSONObject readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static String parseBusinessKeyObjectCode(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        return StringUtils.trimToNull(businessKey.split(":", 2)[0]);
    }

    static Long parseBusinessKeyRecordId(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        String value = StringUtils.trimToNull(businessKey.split(":", 2)[1]);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    record BusinessRuntimeContext(String requestedObjectCode,
                                          String objectCode,
                                          String configKey,
                                          AiBusinessDocumentConfig documentConfig,
                                          AiCrudConfig runtimeConfig,
                                          AiBusinessObject businessObject) {
    }

    record BindingLookupResult(AiBusinessBinding binding,
                                       String matchedObjectCode,
                                       List<String> candidates) {
    }
}
