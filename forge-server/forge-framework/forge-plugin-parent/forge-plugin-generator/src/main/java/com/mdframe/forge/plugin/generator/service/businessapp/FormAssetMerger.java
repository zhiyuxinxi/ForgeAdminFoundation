package com.mdframe.forge.plugin.generator.service.businessapp;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Preserves stable form identity while enriching duplicate catalogs. */
final class FormAssetMerger {
    private FormAssetMerger() {}

    static void appendUniqueFormAssets(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int index = 0; index < target.size(); index++) {
            String key = formAssetIdentity(target.get(index));
            if (StringUtils.isNotBlank(key)) {
                positions.putIfAbsent(key, index);
            }
        }
        for (Map<String, Object> asset : source) {
            String key = formAssetIdentity(asset);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Integer existingIndex = positions.get(key);
            if (existingIndex == null) {
                positions.put(key, target.size());
                target.add(asset);
            } else {
                mergeFormAssetMetadata(target.get(existingIndex), asset);
            }
        }
    }

    static void mergeFormAssetMetadata(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) {
            return;
        }
        List<Map<String, Object>> targetFields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(target.get("fieldCatalog")));
        if (targetFields.isEmpty()) {
            targetFields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(target.get("fields")));
        }
        List<Map<String, Object>> sourceFields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(source.get("fieldCatalog")));
        if (sourceFields.isEmpty()) {
            sourceFields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(source.get("fields")));
        }
        if (sourceFields.size() > targetFields.size()) {
            List<Map<String, Object>> mergedFields = new ArrayList<>(sourceFields);
            target.put("fieldCatalog", mergedFields);
            target.put("fields", mergedFields);
            target.put("fieldCount", mergedFields.size());
            target.put("fieldPreview", FormAssetFields.buildFieldPreview(mergedFields));
        }
        for (String key : List.of("configKey", "objectCode", "objectName", "viewKey",
                "providerKey", "providerName", "formUrl")) {
            if (StringUtils.isBlank(FormAssetValues.textValue(target.get(key))) && StringUtils.isNotBlank(FormAssetValues.textValue(source.get(key)))) {
                target.put(key, source.get(key));
            }
        }
        if (!Boolean.TRUE.equals(FormAssetValues.readNullableBooleanValue(target.get("supportsSave")))
                && Boolean.TRUE.equals(FormAssetValues.readNullableBooleanValue(source.get("supportsSave")))) {
            target.put("supportsSave", true);
        }
    }

    static String formAssetIdentity(Map<String, Object> asset) {
        if (asset == null) {
            return "";
        }
        String formKey = StringUtils.trimToNull(FormAssetValues.textValue(asset.get("formKey")));
        if (formKey == null) {
            return "";
        }
        return StringUtils.defaultIfBlank(FormAssetValues.textValue(asset.get("formMode")), FormAssetValues.textValue(asset.get("type")))
                + "::" + StringUtils.defaultString(FormAssetValues.textValue(asset.get("providerKey")))
                + "::" + formKey;
    }
}
