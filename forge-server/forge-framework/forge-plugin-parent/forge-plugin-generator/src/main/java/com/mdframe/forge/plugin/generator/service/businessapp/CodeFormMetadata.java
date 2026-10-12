package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Provider capabilities and configured display metadata share one overlay policy. */
final class CodeFormMetadata {
    private CodeFormMetadata() {}

    static Map<String, Object> sanitizeCodeAppMetadata(JSONObject metadata, boolean includeInternal) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>(metadata);
        List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(metadata.get("fields"), includeInternal);
        if (fields.isEmpty()) {
            fields = firstCodeAppAssetFields(metadata, includeInternal);
        }
        result.put("fields", fields);

        List<Map<String, Object>> formAssets = FormAssetValues.readMapList(FormAssetValues.readNestedArray(metadata.get("formAssets")));
        if (!formAssets.isEmpty()) {
            List<Map<String, Object>> normalizedAssets = new ArrayList<>();
            for (Map<String, Object> asset : formAssets) {
                Map<String, Object> item = new LinkedHashMap<>(asset);
                List<Map<String, Object>> assetFields = normalizeCodeAppMetadataFields(
                        item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"), includeInternal);
                if (assetFields.isEmpty()) {
                    assetFields = fields;
                }
                item.put("fields", assetFields);
                item.put("fieldCatalog", assetFields);
                item.put("fieldCount", assetFields.size());
                item.put("fieldPreview", buildCodeAppFieldPreview(assetFields));
                normalizedAssets.add(item);
            }
            result.put("formAssets", normalizedAssets);
        }
        return result;
    }

    static List<Map<String, Object>> mergeCodeAppAssets(String objectCode,
                                                        List<Map<String, Object>> providerAssets,
                                                        JSONObject metadata, boolean includeInternal) {
        if (metadata == null || metadata.isEmpty()) {
            return providerAssets == null ? List.of() : providerAssets;
        }
        AssetOverlay overlay = new AssetOverlay(objectCode,
                FormAssetValues.readMapList(FormAssetValues.readNestedArray(metadata.get("formAssets"))),
                FormAssetValues.readStringSet(metadata.get("removedFormAssetKeys")),
                normalizeCodeAppMetadataFields(metadata.get("fields"), includeInternal),
                includeInternal ? Set.of() : collectNonPublicCodeAppFieldCodes(metadata.get("fields")),
                includeInternal);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (Map<String, Object> provider : providerAssets == null ? List.<Map<String, Object>>of() : providerAssets) {
            Map<String, Object> merged = mergeProviderAsset(provider, overlay, used);
            if (merged != null) {
                result.add(merged);
            }
        }
        for (Map<String, Object> configured : overlay.configured()) {
            String key = codeAppAssetKey(configured);
            if (StringUtils.isNotBlank(key) && (overlay.removed().contains(key) || used.contains(key))) {
                continue;
            }
            result.add(configuredAsset(configured, overlay));
        }
        return result;
    }

    private static Map<String, Object> mergeProviderAsset(Map<String, Object> provider,
                                                           AssetOverlay overlay, Set<String> used) {
        if (provider == null) {
            return null;
        }
        String key = codeAppAssetKey(provider);
        if (StringUtils.isNotBlank(key) && overlay.removed().contains(key)) {
            return null;
        }
        Map<String, Object> configured = findConfiguredCodeAppAsset(overlay.configured(), provider);
        Map<String, Object> item = new LinkedHashMap<>(provider);
        if (configured != null) {
            mergeCodeAppAssetDisplay(item, configured);
            String configuredKey = codeAppAssetKey(configured);
            if (StringUtils.isNotBlank(configuredKey)) {
                used.add(configuredKey);
            }
        }
        List<Map<String, Object>> providerFields = FormAssetValues.readMapList(
                FormAssetValues.readNestedArray(assetFieldSource(provider)));
        List<Map<String, Object>> configuredFields = overlay.globalFields();
        if (configuredFields.isEmpty() && configured != null) {
            configuredFields = normalizeCodeAppMetadataFields(assetFieldSource(configured), overlay.includeInternal());
        }
        Set<String> hidden = new LinkedHashSet<>(overlay.hidden());
        if (!overlay.includeInternal() && configured != null) {
            hidden.addAll(collectNonPublicCodeAppFieldCodes(assetFieldSource(configured)));
        }
        List<Map<String, Object>> fields = mergeCodeAppFields(providerFields, configuredFields, hidden);
        if (!fields.isEmpty()) {
            applyAssetFields(item, fields);
        }
        item.put("objectCode", StringUtils.defaultIfBlank(FormAssetValues.textValue(item.get("objectCode")), overlay.objectCode()));
        return item;
    }

    private static Map<String, Object> configuredAsset(Map<String, Object> configured, AssetOverlay overlay) {
        Map<String, Object> item = new LinkedHashMap<>(configured);
        List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(assetFieldSource(item), overlay.includeInternal());
        if (fields.isEmpty()) {
            fields = overlay.globalFields();
        }
        item.put("objectCode", StringUtils.defaultIfBlank(FormAssetValues.textValue(item.get("objectCode")), overlay.objectCode()));
        applyAssetFields(item, fields);
        return item;
    }

    private static Object assetFieldSource(Map<String, Object> asset) {
        return asset.get("fields") == null ? asset.get("fieldCatalog") : asset.get("fields");
    }

    private static void applyAssetFields(Map<String, Object> item, List<Map<String, Object>> fields) {
        item.put("fields", fields);
        item.put("fieldCatalog", fields);
        item.put("fieldCount", fields.size());
        item.put("fieldPreview", buildCodeAppFieldPreview(fields));
        item.put("metadataConfigured", true);
    }

    private record AssetOverlay(String objectCode, List<Map<String, Object>> configured,
                                Set<String> removed, List<Map<String, Object>> globalFields,
                                Set<String> hidden, boolean includeInternal) {
    }

    static String codeAppAssetKey(Map<String, Object> asset) {
        if (asset == null) {
            return null;
        }
        String formKey = StringUtils.trimToNull(FormAssetValues.textValue(asset.get("formKey")));
        if (StringUtils.isNotBlank(formKey)) {
            return "form:" + formKey;
        }
        String providerKey = StringUtils.trimToNull(FormAssetValues.textValue(asset.get("providerKey")));
        return StringUtils.isBlank(providerKey) ? null : "provider:" + providerKey;
    }

    static List<Map<String, Object>> mergeCodeAppFields(List<Map<String, Object>> providerFields,
                                                         List<Map<String, Object>> configuredFields,
                                                         Set<String> hiddenFields) {
        Map<String, Map<String, Object>> configuredMap = indexConfiguredFields(configuredFields);

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> providerField : providerFields == null ? List.<Map<String, Object>>of() : providerFields) {
            String fieldCode = fieldCode(providerField);
            if (fieldCode == null || hiddenFields.contains(fieldCode) || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(providerField);
            Object providerSearchable = providerField.get("searchable");
            Map<String, Object> configured = configuredMap.get(fieldCode);
            if (configured != null) {
                FormAssetValues.mergeNonNull(item, configured);
            }
            if (providerSearchable instanceof Boolean) {
                item.put("searchable", providerSearchable);
            }
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            result.add(item);
        }

        for (Map<String, Object> configured : configuredFields == null ? List.<Map<String, Object>>of() : configuredFields) {
            String fieldCode = fieldCode(configured);
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(configured);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            result.add(item);
        }
        return result;
    }

    static Set<String> collectNonPublicCodeAppFieldCodes(Object source) {
        JSONArray array = FormAssetValues.readNestedArray(source);
        if (array.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject field = array.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(field.getString("field")),
                    StringUtils.trimToNull(field.getString("fieldCode")),
                    StringUtils.trimToNull(field.getString("code")));
            if (fieldCode != null && !isPublicCodeAppField(field)) {
                result.add(fieldCode);
            }
        }
        return result;
    }

    static void mergeCodeAppAssetDisplay(Map<String, Object> target, Map<String, Object> configured) {
        for (String key : List.of("appName", "objectName", "businessName", "formKey", "formName",
                "formMode", "type", "providerKey", "providerName", "formUrl", "description")) {
            String value = StringUtils.trimToNull(FormAssetValues.textValue(configured.get(key)));
            if (value != null) {
                target.put(key, value);
            }
        }
        if (configured.containsKey("supportsSave")) {
            target.put("supportsSave", FormAssetValues.readBooleanValue(configured.get("supportsSave"), true));
        }
    }

    static Map<String, Object> findConfiguredCodeAppAsset(List<Map<String, Object>> configuredAssets,
                                                           Map<String, Object> providerAsset) {
        if (configuredAssets == null || configuredAssets.isEmpty()) {
            return null;
        }
        String formKey = StringUtils.trimToNull(FormAssetValues.textValue(providerAsset.get("formKey")));
        String providerKey = StringUtils.trimToNull(FormAssetValues.textValue(providerAsset.get("providerKey")));
        for (Map<String, Object> asset : configuredAssets) {
            if (asset == null) {
                continue;
            }
            if (StringUtils.isNotBlank(formKey) && StringUtils.equals(formKey, StringUtils.trimToNull(FormAssetValues.textValue(asset.get("formKey"))))) {
                return asset;
            }
            if (StringUtils.isNotBlank(providerKey)
                    && StringUtils.equals(providerKey, StringUtils.trimToNull(FormAssetValues.textValue(asset.get("providerKey"))))) {
                return asset;
            }
        }
        return null;
    }

    static List<Map<String, Object>> firstCodeAppAssetFields(JSONObject metadata, boolean includeInternal) {
        List<Map<String, Object>> assets = FormAssetValues.readMapList(FormAssetValues.readNestedArray(metadata.get("formAssets")));
        for (Map<String, Object> asset : assets) {
            List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(
                    asset.get("fields") == null ? asset.get("fieldCatalog") : asset.get("fields"), includeInternal);
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        return List.of();
    }

    static List<Map<String, Object>> normalizeCodeAppMetadataFields(Object source, boolean includeInternal) {
        JSONArray array = FormAssetValues.readNestedArray(source);
        if (array.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject field = array.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(field.getString("field")),
                    StringUtils.trimToNull(field.getString("fieldCode")),
                    StringUtils.trimToNull(field.getString("code")));
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            item.putIfAbsent("label", fieldCode);
            item.putIfAbsent("componentType", StringUtils.defaultIfBlank(FormAssetValues.textValue(item.get("type")), "input"));
            item.putIfAbsent("type", FormAssetFields.normalizeTaskFormFieldType(FormAssetValues.textValue(item.get("componentType"))));
            item.putIfAbsent("visible", true);
            item.putIfAbsent("readonly", !FormAssetValues.readBooleanValue(item.get("writable"), true));
            if (!includeInternal && !isPublicCodeAppField(item)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    static boolean isPublicCodeAppField(Map<String, Object> field) {
        if (field == null) {
            return false;
        }
        return FormAssetValues.readBooleanValue(field.get("visible"), true)
                && !FormAssetValues.readBooleanValue(field.get("internal"), false)
                && !FormAssetValues.readBooleanValue(field.get("systemField"), false);
    }

    static boolean isPublicCodeAppFormField(Map<String, Object> field) {
        return isPublicCodeAppField(field) && FormAssetValues.readBooleanValue(field.get("formVisible"), true);
    }

    static List<String> buildCodeAppFieldPreview(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        List<String> preview = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String text = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("label"))),
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("field"))));
            if (text != null) {
                preview.add(text);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    private static Map<String, Map<String, Object>> indexConfiguredFields(List<Map<String, Object>> fields) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> field : fields == null ? List.<Map<String, Object>>of() : fields) {
            String code = fieldCode(field);
            if (code != null) {
                result.put(code, field);
            }
        }
        return result;
    }

    private static String fieldCode(Map<String, Object> field) {
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("field"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("fieldCode"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("code"))));
    }
}
