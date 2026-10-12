package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves form assets owned by application page snapshots. */
@Slf4j
final class ApplicationPageFormAssets {
    private final BusinessApplicationService businessApplicationService;

    ApplicationPageFormAssets(BusinessApplicationService businessApplicationService) {
        this.businessApplicationService = businessApplicationService;
    }

    Map<String, Object> collectApplicationPageFormAssets(Long applicationId, String objectCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (applicationId == null || applicationId <= 0 || businessApplicationService == null) {
            return result;
        }
        try {
            var application = businessApplicationService.detail(applicationId);
            JSONObject options = FormAssetValues.readJsonObject(application.getOptions());
            JSONObject builder = FormAssetValues.readNestedObject(options.get("inAppBuilder"));
            JSONArray nodes = FormAssetValues.readNestedArray(builder.get("nodes"));
            JSONObject pages = FormAssetValues.readNestedObject(builder.get("pages"));
            JSONArray assets = FormAssetValues.readNestedArray(builder.get("formAssets"));
            if (nodes.isEmpty() || pages.isEmpty() || assets.isEmpty()) {
                return result;
            }
            Map<String, JSONObject> assetsById = indexAssets(assets);
            List<Map<String, Object>> formAssets = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (!matchesObjectPage(node, objectCode)) {
                    continue;
                }
                for (Map<String, Object> item : collectPageAssets(applicationId, node, pages, assets, assetsById)) {
                    String formKey = StringUtils.trimToNull(FormAssetValues.textValue(item.get("formKey")));
                    if (formKey != null && seen.add(formKey)) {
                        formAssets.add(item);
                    }
                }
            }
            if (formAssets.isEmpty()) {
                return result;
            }
            result.put("applicationId", String.valueOf(applicationId));
            result.put("objectCode", objectCode);
            result.put("formAssets", formAssets);
            result.put("warnings", List.of());
        } catch (Exception error) {
            log.debug("读取应用页面表单资产失败: applicationId={}, objectCode={}", applicationId, objectCode, error);
        }
        return result;
    }

    private Map<String, JSONObject> indexAssets(JSONArray assets) {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset != null && StringUtils.isNotBlank(asset.getString("id"))) {
                result.put(asset.getString("id"), asset);
            }
        }
        return result;
    }

    private boolean matchesObjectPage(JSONObject node, String objectCode) {
        if (node == null || !"page".equalsIgnoreCase(node.getString("type"))) {
            return false;
        }
        JSONObject objectRef = FormAssetValues.readNestedObject(node.get("objectRef"));
        return StringUtils.equals(objectCode,
                StringUtils.firstNonBlank(objectRef.getString("objectCode"), node.getString("objectCode")));
    }

    private Set<String> referencedAssets(JSONObject page, JSONObject node,
                                         JSONObject objectRef, JSONArray assets) {
        Set<String> referenced = new LinkedHashSet<>();
        collectFormAssetIds(page, referenced);
        String direct = StringUtils.firstNonBlank(node.getString("formAssetId"), objectRef.getString("formAssetId"));
        if (direct != null) {
            referenced.add(direct);
        }
        // Historical CRUD pages may use the application's one unambiguous default form.
        if (referenced.isEmpty()) {
            String fallback = resolveDefaultPageFormAssetId(node, objectRef, assets);
            if (fallback != null) {
                referenced.add(fallback);
            }
        }
        return referenced;
    }

    String resolveDefaultPageFormAssetId(JSONObject pageNode,
                                                  JSONObject objectRef,
                                                  JSONArray assets) {
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        String requestedFormKey = StringUtils.firstNonBlank(
                pageNode == null ? null : pageNode.getString("formKey"),
                pageNode == null ? null : pageNode.getString("defaultFormKey"),
                objectRef == null ? null : objectRef.getString("formKey"),
                objectRef == null ? null : objectRef.getString("defaultFormKey"));
        JSONObject requested = findRequestedAsset(assets, requestedFormKey);
        if (requested != null) {
            return StringUtils.trimToNull(requested.getString("id"));
        }
        String markedDefault = null;
        String onlyAsset = null;
        int assetCount = 0;
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset == null || StringUtils.isBlank(asset.getString("id"))) {
                continue;
            }
            assetCount++;
            onlyAsset = asset.getString("id");
            if (Boolean.TRUE.equals(asset.getBoolean("default"))
                    || Boolean.TRUE.equals(asset.getBoolean("isDefault"))) {
                markedDefault = asset.getString("id");
            }
        }
        if (markedDefault != null) {
            return markedDefault;
        }
        return assetCount == 1 ? onlyAsset : null;
    }

    private JSONObject findRequestedAsset(JSONArray assets, String requestedFormKey) {
        if (requestedFormKey == null) {
            return null;
        }
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset != null && StringUtils.equals(requestedFormKey,
                    StringUtils.firstNonBlank(asset.getString("formKey"), asset.getString("id")))) {
                return asset;
            }
        }
        return null;
    }

    Map<String, Object> buildApplicationPageFormAsset(Long applicationId,
                                                               JSONObject pageNode,
                                                               JSONObject objectRef,
                                                               JSONObject source,
                                                               String pageId) {
        Map<String, Object> item = new LinkedHashMap<>();
        String sourceAssetId = StringUtils.defaultIfBlank(source.getString("id"), "default");
        String formKey = "app_" + applicationId + "_page_" + pageId + "_form_" + sourceAssetId;
        String pageName = StringUtils.firstNonBlank(
                pageNode.getString("pageName"),
                pageNode.getString("name"),
                pageNode.getString("title"),
                pageId);
        String objectName = StringUtils.firstNonBlank(
                objectRef.getString("objectName"), pageName, objectRef.getString("objectCode"));
        JSONObject formDesignerSchema = FormAssetValues.readNestedObject(source.get("formDesignerSchema"));
        JSONObject schema = FormAssetValues.readNestedObject(source.get("schema"));
        if (schema.isEmpty()) {
            schema = formDesignerSchema;
        }
        List<Map<String, Object>> fields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(source.get("fieldCatalog")));
        if (fields.isEmpty()) {
            fields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(source.get("fields")));
        }
        if (fields.isEmpty()) {
            fields = FormAssetFields.collectBusinessFormFieldCatalog(schema);
        }
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("applicationId", String.valueOf(applicationId));
        item.put("objectCode", objectRef.getString("objectCode"));
        item.put("objectName", objectName);
        item.put("configKey", objectRef.getString("configKey"));
        item.put("formKey", formKey);
        item.put("sourceFormKey", source.getString("formKey"));
        item.put("formName", StringUtils.firstNonBlank(
                source.getString("formName"), source.getString("name"), pageName, objectName + "表单"));
        item.put("pageId", pageId);
        item.put("pageCode", StringUtils.firstNonBlank(pageNode.getString("pageCode"), pageNode.getString("pageKey"), pageId));
        item.put("pageName", pageName);
        item.put("pageType", pageNode.getString("pageType"));
        item.put("source", "applicationPage");
        item.put("sourceType", "applicationPageForm");
        item.put("fieldCatalog", fields);
        item.put("fields", fields);
        item.put("fieldCount", fields.size());
        item.put("fieldPreview", FormAssetFields.buildFieldPreview(fields));
        item.put("supportsSave", true);
        schema.put("formKey", formKey);
        schema.put("formName", item.get("formName"));
        schema.put("fieldCatalog", fields);
        schema.put("fields", fields);
        item.put("schema", schema);
        return item;
    }

    void collectFormAssetIds(Object value, Set<String> result) {
        if (value instanceof Map<?, ?> map) {
            Object propsValue = map.get("props");
            if (propsValue instanceof Map<?, ?> props && props.get("formAssetId") != null) {
                String id = StringUtils.trimToNull(String.valueOf(props.get("formAssetId")));
                if (id != null) {
                    result.add(id);
                }
            }
            map.values().forEach(child -> collectFormAssetIds(child, result));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(child -> collectFormAssetIds(child, result));
        }
    }

    JSONObject resolveApplicationPageFormAsset(String formKey) {
        String key = StringUtils.trimToNull(formKey);
        if (key == null || businessApplicationService == null || !key.startsWith("app_")) {
            return new JSONObject();
        }
        int pageMarker = key.indexOf("_page_");
        if (pageMarker <= 4) {
            return new JSONObject();
        }
        Long applicationId;
        try {
            applicationId = Long.valueOf(key.substring(4, pageMarker));
        } catch (NumberFormatException error) {
            return new JSONObject();
        }
        try {
            var application = businessApplicationService.detail(applicationId);
            JSONObject options = FormAssetValues.readJsonObject(application.getOptions());
            JSONObject builder = FormAssetValues.readNestedObject(options.get("inAppBuilder"));
            JSONArray nodes = FormAssetValues.readNestedArray(builder.get("nodes"));
            JSONObject pages = FormAssetValues.readNestedObject(builder.get("pages"));
            JSONArray assets = FormAssetValues.readNestedArray(builder.get("formAssets"));
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject resolved = resolveReferencedPageAsset(key, applicationId, nodes.getJSONObject(i), pages, assets);
                if (!resolved.isEmpty()) {
                    return resolved;
                }
            }
            return new JSONObject();
        } catch (Exception error) {
            log.debug("解析应用页面表单资产失败: formKey={}", formKey, error);
            return new JSONObject();
        }
    }

    JSONObject resolveApplicationPageFormSchema(String formKey) {
        JSONObject asset = resolveApplicationPageFormAsset(formKey);
        if (asset.isEmpty()) {
            return new JSONObject();
        }
        JSONObject schema = FormAssetValues.readNestedObject(asset.get("schema"));
        if (schema.isEmpty()) {
            schema.put("formKey", asset.getString("formKey"));
            schema.put("formName", asset.getString("formName"));
            schema.put("fieldCatalog", asset.get("fieldCatalog"));
            schema.put("fields", asset.get("fields"));
        }
        return schema;
    }

    boolean isAvailable() {
        return businessApplicationService != null;
    }

    private List<Map<String, Object>> collectPageAssets(Long applicationId, JSONObject node,
                                                       JSONObject pages, JSONArray assets,
                                                       Map<String, JSONObject> assetsById) {
        String pageId = StringUtils.trimToNull(node.getString("id"));
        JSONObject page = pageId == null ? null : pages.getJSONObject(pageId);
        if (page == null) {
            return List.of();
        }
        JSONObject objectRef = FormAssetValues.readNestedObject(node.get("objectRef"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String assetId : referencedAssets(page, node, objectRef, assets)) {
            JSONObject source = assetsById.get(assetId);
            if (source != null) {
                result.add(buildApplicationPageFormAsset(applicationId, node, objectRef, source, pageId));
            }
        }
        return result;
    }

    private JSONObject resolveReferencedPageAsset(String key, Long applicationId, JSONObject node,
                                                  JSONObject pages, JSONArray assets) {
        if (node == null || !"page".equalsIgnoreCase(node.getString("type"))) {
            return new JSONObject();
        }
        String pageId = StringUtils.trimToNull(node.getString("id"));
        JSONObject page = pageId == null ? null : pages.getJSONObject(pageId);
        if (page == null) {
            return new JSONObject();
        }
        Set<String> referenced = new LinkedHashSet<>();
        collectFormAssetIds(page, referenced);
        JSONObject objectRef = FormAssetValues.readNestedObject(node.get("objectRef"));
        for (int i = 0; i < assets.size(); i++) {
            JSONObject source = assets.getJSONObject(i);
            if (source == null || !referenced.contains(source.getString("id"))) {
                continue;
            }
            JSONObject resolved = FormAssetValues.readNestedObject(buildApplicationPageFormAsset(
                    applicationId, node, objectRef, source, pageId));
            if (StringUtils.equals(key, resolved.getString("formKey"))) {
                return resolved;
            }
        }
        return new JSONObject();
    }
}
