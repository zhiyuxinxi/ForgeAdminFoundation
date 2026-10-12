package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Connects code-form providers and fills the task identity omitted by provider responses. */
final class BusinessCodeTaskContext {
    private final BusinessCodeFormProviderRegistry providers;
    private final BusinessCodeTaskFields fields;
    private final NodeFormConfiguration configuration = new NodeFormConfiguration();

    BusinessCodeTaskContext(BusinessCodeFormProviderRegistry providers, BusinessCodeTaskFields fields) {
        this.providers = providers;
        this.fields = fields;
    }

    record Identity(String objectCode, Long recordId, String businessKey, String configKey) { }

    // Only invoked provider results pass through the subsequent metadata lookup and projection.
    record Result(BusinessTaskFormContextVO context, List<Map<String, Object>> permissions,
                  boolean providerInvoked) { }

    Result read(BusinessTaskFormContextQueryDTO query, JSONObject nodeForm, Identity runtime,
                    JSONObject formRef) {
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        List<Map<String, Object>> permissions = configuration.normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        BusinessTaskFormContextQueryDTO effectiveQuery = enrichTaskFormQuery(query, runtime, nodeForm, formRef);
        BusinessTaskFormContextVO fallback = buildBusinessCodeFormFallback(
                effectiveQuery, nodeForm, runtime, formRef, permissions, providerKey);
        if (StringUtils.isBlank(providerKey)) {
            fallback.getWarnings().add("当前代码表单缺少 providerKey，无法加载业务表单");
            return new Result(fallback, permissions, false);
        }
        return providers.find(providerKey)
                .map(provider -> new Result(mergeBusinessCodeFormBase(
                        provider.buildContext(effectiveQuery, new LinkedHashMap<>(formRef), permissions), fallback), permissions, true))
                .orElseGet(() -> {
                    fallback.getWarnings().add("代码表单Provider未注册: " + providerKey);
                    return new Result(fallback, permissions, false);
                });
    }

    Result save(BusinessTaskFormSaveDTO dto, JSONObject nodeForm, JSONObject formRef) {
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        if (StringUtils.isBlank(providerKey)) {
            throw new BusinessException("当前代码表单缺少 providerKey，无法保存业务字段");
        }
        List<Map<String, Object>> permissions = configuration.normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        BusinessTaskFormContextVO context = providers.require(providerKey)
                .saveContext(dto, new LinkedHashMap<>(formRef), permissions);
        return new Result(context, permissions, true);
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormFallback(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   Identity runtime,
                                                                   JSONObject formRef,
                                                                   List<Map<String, Object>> permissions,
                                                                   String providerKey) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setConfigured(true);
        vo.setFormType("business-code");
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getTaskDefKey()),
                StringUtils.trimToNull(nodeForm.getString("taskDefKey"))));
        vo.setObjectCode(runtime.objectCode());
        vo.setBusinessObjectName(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("objectName")),
                StringUtils.trimToNull(formRef.getString("objectName")),
                StringUtils.trimToNull(formRef.getString("businessName")),
                runtime.objectCode()));
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formKey")),
                StringUtils.trimToNull(formRef.getString("formKey"))));
        vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
        vo.setProviderKey(providerKey);
        vo.setFormUrl(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formUrl")),
                StringUtils.trimToNull(formRef.getString("formUrl"))));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(configuration.normalizeNodeEditMode(nodeForm.getString("editMode")));
        vo.setFormRef(new LinkedHashMap<>(formRef));
        vo.setFieldPermissions(permissions);
        fields.applyApprovalPolicy(vo, nodeForm);
        return vo;
    }

    private BusinessTaskFormContextQueryDTO enrichTaskFormQuery(BusinessTaskFormContextQueryDTO query,
                                                               Identity runtime,
                                                               JSONObject nodeForm,
                                                               JSONObject formRef) {
        BusinessTaskFormContextQueryDTO source = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        BusinessTaskFormContextQueryDTO result = new BusinessTaskFormContextQueryDTO();
        result.setTaskId(StringUtils.trimToNull(source.getTaskId()));
        result.setBusinessKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey())));
        result.setProcessInstanceId(StringUtils.trimToNull(source.getProcessInstanceId()));
        result.setProcessDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getProcessDefKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("processDefKey"))));
        result.setTaskDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getTaskDefKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("taskDefKey"))));
        result.setObjectCode(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getObjectCode()),
                runtime == null ? null : StringUtils.trimToNull(runtime.objectCode())));
        result.setRecordId(source.getRecordId() != null ? source.getRecordId() : runtime == null ? null : runtime.recordId());
        result.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getFormKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("formKey")),
                formRef == null ? null : StringUtils.trimToNull(formRef.getString("formKey"))));
        return result;
    }

    private BusinessTaskFormContextVO mergeBusinessCodeFormBase(BusinessTaskFormContextVO source,
                                                               BusinessTaskFormContextVO fallback) {
        if (source == null) {
            return fallback;
        }
        if (source.getConfigured() == null) {
            source.setConfigured(true);
        }
        if (StringUtils.isBlank(source.getFormType())) {
            source.setFormType("business-code");
        }
        if (StringUtils.isBlank(source.getTaskId())) {
            source.setTaskId(fallback.getTaskId());
        }
        if (StringUtils.isBlank(source.getBusinessKey())) {
            source.setBusinessKey(fallback.getBusinessKey());
        }
        if (StringUtils.isBlank(source.getProcessInstanceId())) {
            source.setProcessInstanceId(fallback.getProcessInstanceId());
        }
        if (StringUtils.isBlank(source.getProcessDefKey())) {
            source.setProcessDefKey(fallback.getProcessDefKey());
        }
        if (StringUtils.isBlank(source.getTaskDefKey())) {
            source.setTaskDefKey(fallback.getTaskDefKey());
        }
        if (StringUtils.isBlank(source.getObjectCode())) {
            source.setObjectCode(fallback.getObjectCode());
        }
        if (StringUtils.isBlank(source.getBusinessObjectName())) {
            source.setBusinessObjectName(fallback.getBusinessObjectName());
        }
        if (StringUtils.isBlank(source.getBusinessSummary())) {
            source.setBusinessSummary(fallback.getBusinessSummary());
        }
        if (source.getRecordId() == null) {
            source.setRecordId(fallback.getRecordId());
        }
        if (StringUtils.isBlank(source.getConfigKey())) {
            source.setConfigKey(fallback.getConfigKey());
        }
        if (StringUtils.isBlank(source.getFormKey())) {
            source.setFormKey(fallback.getFormKey());
        }
        if (StringUtils.isBlank(source.getFormName())) {
            source.setFormName(fallback.getFormName());
        }
        if (StringUtils.isBlank(source.getProviderKey())) {
            source.setProviderKey(fallback.getProviderKey());
        }
        if (StringUtils.isBlank(source.getFormUrl())) {
            source.setFormUrl(fallback.getFormUrl());
        }
        if (StringUtils.isBlank(source.getViewKey())) {
            source.setViewKey(fallback.getViewKey());
        }
        if (StringUtils.isBlank(source.getEditMode())) {
            source.setEditMode(fallback.getEditMode());
        }
        if (source.getFormRef() == null || source.getFormRef().isEmpty()) {
            source.setFormRef(fallback.getFormRef());
        }
        if (source.getFieldPermissions() == null || source.getFieldPermissions().isEmpty()) {
            source.setFieldPermissions(fallback.getFieldPermissions());
        }
        return source;
    }

}
