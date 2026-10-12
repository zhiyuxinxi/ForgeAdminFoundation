package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.booleanValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.safeKey;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;

/** Model and page reference fields with security and display metadata. */
final class RuntimeFieldCatalog {

    private RuntimeFieldCatalog() {
    }

    static LowcodeFieldSchema findField(LowcodeModelSchema modelSchema, String fieldName) {
        if (modelSchema == null || modelSchema.getFields() == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        return modelSchema.getFields().stream()
                .filter(field -> fieldName.equals(field.getField()))
                .findFirst()
                .orElse(null);
    }

    static List<Map<String, Object>> buildDictConfig(LowcodeModelSchema modelSchema) {
        Map<String, String> dictMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isNotBlank(field.getDictType())) {
                dictMap.putIfAbsent(field.getDictType(), StringUtils.defaultIfBlank(field.getLabel(), field.getDictType()));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : dictMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dictType", entry.getKey());
            item.put("dictName", entry.getValue());
            item.put("isNew", false);
            item.put("items", List.of());
            result.add(item);
        }
        return result;
    }

    static Map<String, Object> buildDesensitizeConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String sensitiveType = StringUtils.defaultIfBlank(field.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT);
            if ("NONE".equals(sensitiveType)) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", sensitiveType);
            rule.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            result.put(field.getField(), rule);
        }
        return result;
    }

    static Map<String, Object> buildEncryptConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isBlank(field.getEncryptAlgorithm())) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("algorithm", field.getEncryptAlgorithm());
            result.put(field.getField(), rule);
        }
        return result;
    }

    static LowcodeFieldSchema findRuntimeField(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null) {
            return null;
        }
        LowcodeFieldSchema field = findField(modelSchema, fieldName);
        if (field != null) {
            return field;
        }
        Map<String, LowcodeFieldSchema> fieldMap = buildRuntimeFieldMap(modelSchema, pageSchema);
        return fieldMap.get(fieldName);
    }

    static boolean isSystemField(LowcodeFieldSchema field) {
        return field != null && Boolean.TRUE.equals(field.getSystemField());
    }

    static List<LowcodeFieldSchema> resolveFields(LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema,
                                                   String zoneKey,
                                                   Predicate<LowcodeFieldSchema> fallbackPredicate) {
        Map<String, LowcodeFieldSchema> fieldMap = buildRuntimeFieldMap(modelSchema, pageSchema);
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || Boolean.FALSE.equals(zone.getEnabled()) || zone.getFieldRefs() == null || zone.getFieldRefs().isEmpty()) {
            return fieldMap.values().stream()
                    .filter(RuntimeFieldCatalog::isActiveField)
                    .filter(fallbackPredicate)
                    .toList();
        }

        Set<String> refs = new LinkedHashSet<>(zone.getFieldRefs());
        List<LowcodeFieldSchema> selectedFields = refs.stream()
                .map(fieldMap::get)
                .filter(field -> field != null)
                .filter(field -> isZoneFieldAllowed(field, zoneKey, fallbackPredicate))
                .toList();
        if (selectedFields.isEmpty()) {
            return fieldMap.values().stream()
                    .filter(RuntimeFieldCatalog::isActiveField)
                    .filter(fallbackPredicate)
                    .toList();
        }
        return selectedFields;
    }

    static Map<String, LowcodeFieldSchema> buildRuntimeFieldMap(LowcodeModelSchema modelSchema,
                                                                  LowcodePageSchema pageSchema) {
        Map<String, LowcodeFieldSchema> fieldMap = modelSchema.getFields().stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return fieldMap;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> source : ref.getFields()) {
                LowcodeFieldSchema field = buildPageRefField(ref, source);
                if (field != null) {
                    fieldMap.putIfAbsent(field.getField(), field);
                }
            }
        }
        return fieldMap;
    }

    static LowcodeFieldSchema buildPageRefField(LowcodePageModelRef ref, Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        if (StringUtils.isBlank(sourceField)) {
            return null;
        }
        boolean primary = Boolean.TRUE.equals(ref.getPrimary());
        String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                primary ? sourceField : safeKey(ref.getModelCode()) + "__" + sourceField);
        if (StringUtils.isBlank(fieldRef)) {
            return null;
        }

        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(fieldRef);
        field.setColumnName(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField));
        String rawLabel = StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField));
        field.setLabel(rawLabel);
        field.setLabelI18n(displayTranslations(source.get("labelI18n")));
        field.setPlaceholderI18n(displayTranslations(source.get("placeholderI18n")));
        field.setDataType(StringUtils.defaultIfBlank(text(source.get("dataType")), "varchar"));
        field.setLength(integerValue(source.get("length")));
        field.setPrecision(integerValue(source.get("precision")));
        field.setRequired(Boolean.TRUE.equals(booleanValue(source.get("required"))));
        field.setDefaultValue(source.get("defaultValue"));
        field.setSearchable(Boolean.TRUE.equals(booleanValue(source.get("searchable"))));
        field.setListVisible(booleanValue(source.get("listVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("listVisible"))));
        field.setFormVisible(booleanValue(source.get("formVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("formVisible"))));
        field.setComponentType(StringUtils.defaultIfBlank(text(source.get("componentType")), "input"));
        field.setQueryType(StringUtils.defaultIfBlank(text(source.get("queryType")), "eq"));
        field.setDictType(text(source.get("dictType")));
        field.setSensitiveType(StringUtils.defaultIfBlank(text(source.get("sensitiveType")), "NONE"));
        field.setEncryptAlgorithm(text(source.get("encryptAlgorithm")));
        field.setSortable(Boolean.TRUE.equals(booleanValue(source.get("sortable"))));
        field.setPrimaryKey(Boolean.TRUE.equals(booleanValue(source.get("primaryKey"))));
        field.setSystemField(Boolean.TRUE.equals(booleanValue(source.get("systemField"))));
        field.setReadonly(Boolean.TRUE.equals(booleanValue(source.get("readonly"))));
        field.setFieldStatus(StringUtils.defaultIfBlank(text(source.get("fieldStatus")), "ENABLED"));
        field.setAutoIncrement(Boolean.TRUE.equals(booleanValue(source.get("autoIncrement"))));
        field.setWidth(integerValue(source.get("width")));
        field.setRemark(text(source.get("remark")));
        Object basicProps = source.get("basicProps");
        if (basicProps instanceof Map<?, ?> basicPropsMap) {
            Map<String, Object> props = new LinkedHashMap<>();
            basicPropsMap.forEach((key, value) -> {
                if (key != null) {
                    props.put(String.valueOf(key), value);
                }
            });
            field.setBasicProps(props);
        }
        return field;
    }

    static Map<String, String> displayTranslations(Object value) {
        Map<String, String> translations = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (String locale : List.of("zh-CN", "en-US")) {
                if (map.get(locale) instanceof String label && !label.isBlank()) translations.put(locale, label);
            }
        }
        return translations;
    }

    static boolean isZoneFieldAllowed(LowcodeFieldSchema field,
                                       String zoneKey,
                                       Predicate<LowcodeFieldSchema> fallbackPredicate) {
        if (!isActiveField(field)) {
            return false;
        }
        if ("search".equals(zoneKey)) {
            return !isSystemField(field);
        }
        if ("edit".equals(zoneKey)) {
            return !isSystemField(field)
                    && !Boolean.TRUE.equals(field.getReadonly())
                    && fallbackPredicate.test(field);
        }
        return fallbackPredicate.test(field);
    }

    static boolean isActiveField(LowcodeFieldSchema field) {
        String status = StringUtils.defaultString(field == null ? null : field.getFieldStatus());
        return !"DISABLED".equalsIgnoreCase(status) && !"HIDDEN".equalsIgnoreCase(status);
    }

    static LowcodePageZone findZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return null;
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zoneKey.equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
    }
}
