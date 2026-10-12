package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 代码业务表单 Provider 注册表。
 */
@Component
@RequiredArgsConstructor
public class BusinessCodeFormProviderRegistry {

    private final ObjectProvider<? extends com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider> providers;

    public Optional<BusinessCodeFormProvider> find(String providerKey) {
        if (StringUtils.isBlank(providerKey)) {
            return Optional.empty();
        }
        return providers.orderedStream()
                .map(CodeFormProviderCompatibility::legacy)
                .filter(provider -> StringUtils.equals(providerKey, provider.providerKey()))
                .findFirst();
    }

    public BusinessCodeFormProvider require(String providerKey) {
        return find(providerKey)
                .orElseThrow(() -> new BusinessException(UiMessages.translate("代码表单Provider未注册: ") + providerKey));
    }

    public List<Map<String, Object>> listAssets(String objectCode) {
        return listAssets(objectCode, false);
    }

    public List<Map<String, Object>> listAssets(String objectCode, boolean includeInternal) {
        List<Map<String, Object>> result = new ArrayList<>();
        providers.orderedStream().forEach(provider -> {
            List<Map<String, Object>> assets = provider.formAssets(objectCode);
            if (assets == null || assets.isEmpty()) {
                return;
            }
            for (Map<String, Object> source : assets) {
                if (source == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(source);
                item.putIfAbsent("type", "BUSINESS_CODE_FORM");
                item.putIfAbsent("formMode", "BUSINESS_CODE_FORM");
                item.putIfAbsent("objectCode", objectCode);
                item.putIfAbsent("providerKey", provider.providerKey());
                item.putIfAbsent("providerName", provider.providerName());
                item.putIfAbsent("supportsSave", true);
                item.putIfAbsent("sourceType", "codeProvider");
                List<Map<String, Object>> fields = normalizePublicFields(
                        item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"), includeInternal);
                item.put("fields", fields);
                item.put("fieldCatalog", fields);
                item.put("fieldCount", fields.size());
                item.put("fieldPreview", buildFieldPreview(fields));
                result.add(item);
            }
        });
        return result;
    }

    public List<Map<String, Object>> listProviderCatalog(String objectCode) {
        return listProviderCatalog(objectCode, false);
    }

    public List<Map<String, Object>> listProviderCatalog(String objectCode, boolean includeInternal) {
        List<Map<String, Object>> result = new ArrayList<>();
        providers.orderedStream().forEach(provider -> {
            List<Map<String, Object>> assets = normalizeProviderAssets(provider, provider.formAssets(objectCode), objectCode, includeInternal);
            if (assets.isEmpty() && StringUtils.isNotBlank(objectCode)) {
                assets = normalizeProviderAssets(provider, provider.formAssets(null), objectCode, includeInternal);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("providerKey", provider.providerKey());
            item.put("providerName", provider.providerName());
            item.put("assets", assets);
            item.put("assetCount", assets.size());
            if (!assets.isEmpty()) {
                Map<String, Object> firstAsset = assets.get(0);
                item.put("defaultFormKey", firstAsset.get("formKey"));
                item.put("defaultFormName", firstAsset.get("formName"));
                item.put("defaultFormUrl", firstAsset.get("formUrl"));
                item.put("fieldCount", firstAsset.get("fieldCount"));
                item.put("fieldPreview", firstAsset.get("fieldPreview"));
            } else {
                item.put("fieldCount", 0);
                item.put("fieldPreview", List.of());
            }
            result.add(item);
        });
        return result;
    }

    private List<Map<String, Object>> normalizeProviderAssets(com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider provider,
                                                              List<Map<String, Object>> sourceAssets,
                                                              String objectCode,
                                                              boolean includeInternal) {
        if (sourceAssets == null || sourceAssets.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> source : sourceAssets) {
            if (source == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(source);
            item.putIfAbsent("type", "BUSINESS_CODE_FORM");
            item.putIfAbsent("formMode", "BUSINESS_CODE_FORM");
            item.putIfAbsent("objectCode", objectCode);
            item.putIfAbsent("providerKey", provider.providerKey());
            item.putIfAbsent("providerName", provider.providerName());
            item.putIfAbsent("supportsSave", true);
            item.putIfAbsent("sourceType", "codeProvider");
            List<Map<String, Object>> fields = normalizePublicFields(
                    item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"), includeInternal);
            item.put("fields", fields);
            item.put("fieldCatalog", fields);
            item.put("fieldCount", fields.size());
            item.put("fieldPreview", buildFieldPreview(fields));
            String key = assetKey(item);
            if (StringUtils.isNotBlank(key) && !seen.add(key)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    private String assetKey(Map<String, Object> asset) {
        String formKey = StringUtils.trimToNull(asset.get("formKey") == null ? null : String.valueOf(asset.get("formKey")));
        if (StringUtils.isNotBlank(formKey)) {
            return "form:" + formKey;
        }
        String providerKey = StringUtils.trimToNull(asset.get("providerKey") == null ? null : String.valueOf(asset.get("providerKey")));
        return StringUtils.isBlank(providerKey) ? null : "provider:" + providerKey;
    }

    private List<Map<String, Object>> normalizePublicFields(Object source, boolean includeInternal) {
        if (!(source instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            map.forEach((key, fieldValue) -> item.put(String.valueOf(key), fieldValue));
            if (!includeInternal && (readBoolean(item.get("internal")) || readBoolean(item.get("systemField")))) {
                continue;
            }
            item.putIfAbsent("fieldCode", item.get("field"));
            result.add(item);
        }
        return result;
    }

    private List<String> buildFieldPreview(List<Map<String, Object>> fields) {
        List<String> preview = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            Object label = field.get("label");
            Object code = field.get("field");
            String text = StringUtils.trimToNull(label == null ? null : String.valueOf(label));
            if (text == null) {
                text = StringUtils.trimToNull(code == null ? null : String.valueOf(code));
            }
            if (text != null) {
                preview.add(text);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }
}
