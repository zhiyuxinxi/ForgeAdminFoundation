package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Composes the page, low-code, registry, and provider form catalogs. */
final class BusinessFormAssetCatalog {
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final CodeAppMetadataStore metadataStore;
    final ApplicationPageFormAssets pages;
    final RuntimeBusinessForms runtime;

    BusinessFormAssetCatalog(BusinessObjectMapper objects, BusinessCodeFormProviderRegistry providers,
                             CodeAppMetadataStore metadataStore,
                             ApplicationPageFormAssets pages, RuntimeBusinessForms runtime) {
        this.businessObjectMapper = objects;
        this.codeFormProviderRegistry = providers;
        this.metadataStore = metadataStore;
        this.pages = pages;
        this.runtime = runtime;
    }

    Map<String, Object> getFormAssets(String objectCode,
                                             boolean includeInternal,
                                             Long applicationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectCode", objectCode);
        result.put("formAssets", List.of());
        result.put("providerCatalog", List.of());
        result.put("warnings", List.of());
        if (StringUtils.isBlank(objectCode)) {
            return result;
        }

        if (applicationId != null && applicationId > 0 && pages.isAvailable()) {
            Map<String, Object> applicationAssets = pages.collectApplicationPageFormAssets(
                    applicationId, objectCode);
            if (!applicationAssets.isEmpty()) {
                result.putAll(applicationAssets);
                result.put("providerCatalog", codeFormProviderRegistry.listProviderCatalog(objectCode, includeInternal));
                return result;
            }
            result.put("warnings", List.of("当前应用尚未配置属于该业务对象的表单页面，请先在应用中新增表单页面"));
            result.put("providerCatalog", codeFormProviderRegistry.listProviderCatalog(objectCode, includeInternal));
            return result;
        }

        Long tenantId = BusinessFlowRuntimeMetadata.resolveTenantId();
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> providerCatalog = codeFormProviderRegistry.listProviderCatalog(objectCode, includeInternal);
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        if (objects == null || objects.isEmpty()) {
            AiCrudConfig runtimeConfig = runtime.resolvePublishedRuntimeConfig(tenantId, objectCode);
            JSONObject metadata = metadataStore.read(tenantId, objectCode);
            List<Map<String, Object>> assets = new ArrayList<>(runtime.collectRuntimeCrudFormAssets(null, runtimeConfig));
            FormAssetMerger.appendUniqueFormAssets(assets, CodeFormMetadata.mergeCodeAppAssets(
                    objectCode, codeFormProviderRegistry.listAssets(objectCode, includeInternal), metadata, includeInternal));
            if (assets.isEmpty()) {
                warnings.add(UiMessages.translate("业务对象不存在或无权限访问，且未找到代码表单资产: ") + objectCode);
            } else if (runtimeConfig == null) {
                warnings.add(UiMessages.translate("当前编码未匹配低代码业务对象，仅显示代码表单资产"));
            }
            result.put("formAssets", assets);
            if (runtimeConfig != null) {
                result.put("objectCode", StringUtils.defaultIfBlank(runtimeConfig.getObjectCode(), objectCode));
                result.put("objectName", StringUtils.defaultIfBlank(runtimeConfig.getObjectName(), runtimeConfig.getAppName()));
                result.put("configKey", runtimeConfig.getConfigKey());
            }
            result.put("providerCatalog", providerCatalog);
            result.put("codeAppMetadata", CodeFormMetadata.sanitizeCodeAppMetadata(metadata, includeInternal));
            result.put("warnings", warnings);
            return result;
        }

        BusinessObjectVO object = objects.get(0);
        AiCrudConfig runtimeConfig = runtime.resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                object.getConfigKey(), object.getObjectCode(), objectCode));
        JSONObject designerOptions = FormAssetValues.readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = FormAssetValues.readNestedObject(designerOptions.get("formDesignerSchema"));
        List<Map<String, Object>> assets = new ArrayList<>(runtime.collectBusinessFormAssets(object, formSchema));
        FormAssetMerger.appendUniqueFormAssets(assets, runtime.collectRuntimeCrudFormAssets(object, runtimeConfig));
        runtime.appendObjectFieldRegistryFallback(assets, object);
        JSONObject metadata = metadataStore.read(tenantId, object.getObjectCode());
        FormAssetMerger.appendUniqueFormAssets(assets, CodeFormMetadata.mergeCodeAppAssets(
                object.getObjectCode(), codeFormProviderRegistry.listAssets(object.getObjectCode(), includeInternal),
                metadata, includeInternal));
        if (assets.isEmpty()) {
            warnings.add(UiMessages.translate("业务对象尚未配置低代码表单资产"));
        }
        result.put("objectId", object.getId());
        result.put("objectCode", object.getObjectCode());
        result.put("objectName", object.getObjectName());
        result.put("configKey", runtimeConfig == null ? object.getConfigKey() : runtimeConfig.getConfigKey());
        result.put("formAssets", assets);
        result.put("providerCatalog", providerCatalog);
        result.put("codeAppMetadata", CodeFormMetadata.sanitizeCodeAppMetadata(metadata, includeInternal));
        result.put("warnings", warnings);
        return result;
    }

    JSONObject resolveBusinessTaskFormAsset(String objectCode, String formKey) {
        JSONObject applicationAsset = pages.resolveApplicationPageFormAsset(formKey);
        if (!applicationAsset.isEmpty()
                && StringUtils.equals(objectCode, StringUtils.trimToNull(applicationAsset.getString("objectCode")))) {
            return applicationAsset;
        }
        List<Map<String, Object>> assets = collectTaskFormAssets(objectCode);
        if (assets.isEmpty()) {
            return new JSONObject();
        }
        if (StringUtils.isNotBlank(formKey)) {
            for (Map<String, Object> asset : assets) {
                if (StringUtils.equals(formKey, StringUtils.trimToNull(FormAssetValues.textValue(asset.get("formKey"))))) {
                    return FormAssetValues.readNestedObject(asset);
                }
            }
        }
        return assets.size() == 1 ? FormAssetValues.readNestedObject(assets.get(0)) : new JSONObject();
    }

    List<Map<String, Object>> collectTaskFormAssets(String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return List.of();
        }
        Long tenantId = BusinessFlowRuntimeMetadata.resolveTenantId();
        BusinessObjectVO object = queryBusinessObject(tenantId, objectCode);
        List<Map<String, Object>> assets = new ArrayList<>();
        if (object != null) {
            AiCrudConfig runtimeConfig = runtime.resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    object.getConfigKey(), object.getObjectCode(), objectCode));
            JSONObject designerOptions = FormAssetValues.readJsonObject(object.getDesignerOptions());
            JSONObject formSchema = FormAssetValues.readNestedObject(designerOptions.get("formDesignerSchema"));
            assets.addAll(runtime.collectBusinessFormAssets(object, formSchema));
            FormAssetMerger.appendUniqueFormAssets(assets, runtime.collectRuntimeCrudFormAssets(object, runtimeConfig));
            runtime.appendObjectFieldRegistryFallback(assets, object);
            JSONObject metadata = metadataStore.read(tenantId, object.getObjectCode());
            FormAssetMerger.appendUniqueFormAssets(assets, CodeFormMetadata.mergeCodeAppAssets(
                    object.getObjectCode(), codeFormProviderRegistry.listAssets(object.getObjectCode()),
                    metadata, false));
        } else {
            AiCrudConfig runtimeConfig = runtime.resolvePublishedRuntimeConfig(tenantId, objectCode);
            FormAssetMerger.appendUniqueFormAssets(assets, runtime.collectRuntimeCrudFormAssets(null, runtimeConfig));
            JSONObject metadata = metadataStore.read(tenantId, objectCode);
            FormAssetMerger.appendUniqueFormAssets(assets, CodeFormMetadata.mergeCodeAppAssets(
                    objectCode, codeFormProviderRegistry.listAssets(objectCode), metadata, false));
        }
        runtime.appendObjectFieldRegistryFallback(assets, object);
        return assets;
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
}
