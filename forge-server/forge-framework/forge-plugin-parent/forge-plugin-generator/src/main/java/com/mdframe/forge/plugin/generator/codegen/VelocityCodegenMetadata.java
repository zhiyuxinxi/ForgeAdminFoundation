package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratedLowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenMetadata.Model;

@Slf4j
@RequiredArgsConstructor
final class VelocityCodegenMetadata {
    private final GenTableColumnMapper genTableColumnMapper;
    private final ObjectMapper objectMapper;
    private final VelocityCodegenConfiguration configuration;
    private static final String DEFAULT_PACKAGE = "com.mdframe.forge";
    private static final String DEFAULT_AUTHOR = "Forge Generator";

    Model prepare(AiCrudConfig config, GeneratedLowcodeRuntimeConfigBuilder runtimeBuilder) {
        String outputConfigKey = config.getConfigKey();
        String apiBase = resolveApiBase(config);
        AiCrudConfig generated = runtimeBuilder.build(config, apiBase);
        String runtimeConfigKey = generated.getConfigKey();
        String moduleName = StringUtils.defaultIfBlank(configuration.readOption(generated, "moduleName", null),
                resolveModuleName(apiBase));
        String businessPath = resolveBusinessPath(apiBase);
        GenTable table = buildGenTable(generated, moduleName, businessPath);
        List<GenTableColumn> columns = loadColumns(generated);
        applyDisplayTranslations(generated, columns);
        table.setColumns(columns);
        table.setPkColumn(GenUtils.getPkColumn(columns));
        LowcodePageSchema page = configuration.parsePageSchema(generated);
        String layout = StringUtils.firstNonBlank(page == null ? null : page.getLayoutType(),
                generated.getLayoutType(), "simple-crud");
        Map<String, Object> options = configuration.parseJsonObject(generated.getOptions());
        return new Model(generated, table, outputConfigKey, runtimeConfigKey, apiBase, moduleName, businessPath,
                page, layout, options, configuration.readNestedMap(options, "treeConfig"),
                configuration.readNestedMap(options, "masterDetailConfig"));
    }

    record Model(AiCrudConfig config, GenTable table, String outputConfigKey, String runtimeConfigKey, String apiBase,
                 String moduleName, String businessPath, LowcodePageSchema pageSchema, String layoutType,
                 Map<String, Object> options, Map<String, Object> treeConfig,
                 Map<String, Object> masterDetailConfig) {}

    String resolveApiBase(AiCrudConfig config) {
        try {
            String businessApiBase = configuration.readOption(config, "businessApiBase", null);
            if (StringUtils.isNotBlank(businessApiBase)) {
                return normalizeApiBase(businessApiBase);
            }
            Map<String, Object> apiConf = configuration.parseJsonObject(config.getApiConfig());
            String listUrl = (String) apiConf.get("list");
            if (StringUtils.isBlank(listUrl)) return "/" + config.getConfigKey();

            // 去掉 "get@" 前缀
            String url = listUrl.contains("@") ? listUrl.split("@")[1] : listUrl;
            // 去掉 /page 后缀
            if (url.endsWith("/page")) {
                url = url.substring(0, url.length() - 5);
            }
            // 如果是 /ai/crud/xxx 通用路由，则降级为 configKey
            if (url.startsWith("/ai/crud/") || url.startsWith("/rest/")) {
                return "/" + config.getConfigKey().replace("_", "/");
            }
            return normalizeApiBase(url);
        } catch (Exception e) {
            return "/" + config.getConfigKey().replace("_", "/");
        }
    }

    String normalizeApiBase(String apiBase) {
        String normalized = StringUtils.defaultIfBlank(apiBase, "")
                .replace("\\", "/")
                .replaceAll("/{2,}", "/")
                .replaceAll("/+$", "");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    String resolveModuleName(String apiBase) {
        String[] parts = apiBase.split("/");
        for (String p : parts) {
            if (StringUtils.isNotBlank(p)) return p;
        }
        return "app";
    }

    String resolveBusinessPath(String apiBase) {
        String[] parts = apiBase.split("/");
        List<String> nonEmpty = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.isNotBlank(p)) nonEmpty.add(p);
        }
        if (nonEmpty.size() <= 1) return nonEmpty.isEmpty() ? "data" : nonEmpty.get(0);
        return String.join("/", nonEmpty.subList(1, nonEmpty.size()));
    }

    GenTable buildGenTable(AiCrudConfig config, String moduleName, String businessPath) {
        GenTable table = new GenTable();
        table.setTableName(config.getTableName());
        table.setTableComment(StringUtils.isNotBlank(config.getTableComment()) ? config.getTableComment() : config.getConfigKey());
        table.setFunctionName(table.getTableComment());
        table.setFunctionNameEn(configuration.readOption(config, "functionNameEn", null));
        if (StringUtils.isNotBlank(config.getModelSchema())) {
            Map<String, Object> names = configuration.readNestedMap(configuration.parseJsonObject(config.getModelSchema()), "businessNameI18n");
            table.setFunctionNameEn(StringUtils.defaultIfBlank(configuration.text(names.get("en-US")), table.getFunctionNameEn()));
        }

        String className = LowcodeCodegenOptionUtils.buildClassName(
                config.getTableName(), configuration.readOption(config, "entityPrefix", ""),
                configuration.resolveStripTablePrefixes(config));
        table.setClassName(className);
        table.setBusinessName(StringUtils.uncapitalize(className));

        // 包路径：从 options 中读取，或使用默认值
        String packageName = normalizeBasePackageName(configuration.readOption(config, "packageName", DEFAULT_PACKAGE), moduleName);
        table.setPackageName(packageName);
        table.setModuleName(moduleName);
        table.setBusinessName(resolveBusinessName(businessPath));
        table.setAuthor(configuration.readOption(config, "author", DEFAULT_AUTHOR));

        return table;
    }

    String resolveBusinessName(String businessPath) {
        String[] parts = businessPath.split("/");
        return parts[parts.length - 1];
    }

    String normalizeBasePackageName(String packageName, String moduleName) {
        String normalized = StringUtils.defaultIfBlank(packageName, DEFAULT_PACKAGE)
                .replaceAll("\\.+$", "");
        String module = StringUtils.defaultString(moduleName).trim();
        if (StringUtils.isBlank(module)) {
            return normalized;
        }
        String suffix = "." + module;
        if (normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        return normalized;
    }

    List<GenTableColumn> loadColumns(AiCrudConfig config) {
        String tableName = config.getTableName();
        if (StringUtils.isBlank(tableName)) return new ArrayList<>();
        try {
            List<GenTableColumn> columns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
            if (columns == null || columns.isEmpty()) return loadColumnsFromModelSchema(config);
            columns.forEach(GenUtils::initColumnField);
            return columns;
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 加载字段失败, tableName={}", tableName, e);
            return loadColumnsFromModelSchema(config);
        }
    }

    void applyDisplayTranslations(AiCrudConfig config, List<GenTableColumn> columns) {
        Map<String, GenTableColumn> configured = new LinkedHashMap<>();
        List<GenTableColumn> saved = genTableColumnMapper.selectConfiguredColumnsByTableName(config.getTableName());
        if (saved != null) saved.forEach(column -> configured.put(column.getColumnName(), column));
        Map<String, Map<String, Object>> fields = translationFields(configuration.parseJsonObject(config.getModelSchema()));
        for (GenTableColumn column : columns) {
            GenTableColumn savedColumn = configured.get(column.getColumnName());
            if (savedColumn != null) {
                column.setColumnCommentEn(savedColumn.getColumnCommentEn());
                column.setDictLabelsI18n(savedColumn.getDictLabelsI18n());
            }
            Map<String, Object> field = fields.get(column.getColumnName());
            if (field != null) {
                String english = configuration.text(configuration.readNestedMap(field, "labelI18n").get("en-US"));
                if (StringUtils.isNotBlank(english)) column.setColumnCommentEn(english);
                if (field.get("dictLabelsI18n") instanceof Map<?, ?> labels) {
                    Map<String, Map<String, String>> translated = new LinkedHashMap<>();
                    labels.forEach((value, locales) -> {
                        if (locales instanceof Map<?, ?> map) {
                            Map<String, String> texts = new LinkedHashMap<>();
                            map.forEach((locale, label) -> texts.put(String.valueOf(locale), String.valueOf(label)));
                            translated.put(String.valueOf(value), texts);
                        }
                    });
                    column.setDictLabelsI18n(translated);
                }
            }
        }
    }

    List<GenTableColumn> loadColumnsFromModelSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return new ArrayList<>();
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            List<GenTableColumn> columns = new ArrayList<>();
            int sort = 0;
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null || StringUtils.isBlank(field.getColumnName())) {
                    continue;
                }
                GenTableColumn column = modelColumn(field, sort++);
                columns.add(column);
            }
            return columns;
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 从低代码模型协议构建字段失败, configKey={}", config.getConfigKey(), e);
            return new ArrayList<>();
        }
    }

    String toColumnType(LowcodeFieldSchema field) {
        String dataType = StringUtils.defaultIfBlank(field.getDataType(), "varchar").toLowerCase(Locale.ROOT);
        Integer length = field.getLength();
        Integer precision = field.getPrecision();
        return switch (dataType) {
            case "bigint" -> "bigint";
            case "int", "integer" -> "int";
            case "tinyint" -> "tinyint";
            case "decimal" -> "decimal(" + (length == null ? 18 : length) + "," + (precision == null ? 2 : precision) + ")";
            case "datetime" -> "datetime";
            case "date" -> "date";
            case "text" -> "text";
            case "char" -> "char(" + (length == null ? 1 : length) + ")";
            default -> "varchar(" + (length == null || length <= 0 ? 128 : length) + ")";
        };
    }

    String toJavaType(String dataType) {
        String type = StringUtils.defaultIfBlank(dataType, "varchar").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "bigint" -> "Long";
            case "int", "integer", "tinyint" -> "Integer";
            case "decimal" -> "BigDecimal";
            case "datetime" -> "LocalDateTime";
            case "date" -> "LocalDateTime";
            default -> "String";
        };
    }

    String toHtmlType(String componentType, String dataType) {
        String component = StringUtils.defaultString(componentType);
        if ("textarea".equals(component)) {
            return "textarea";
        }
        if ("select".equals(component)) {
            return "select";
        }
        if ("switch".equals(component)) {
            return "radio";
        }
        if ("date".equals(component) || "datetime".equals(component)
                || "date".equals(dataType) || "datetime".equals(dataType)) {
            return "datetime";
        }
        if ("fileUpload".equals(component)) {
            return "fileUpload";
        }
        if ("imageUpload".equals(component)) {
            return "imageUpload";
        }
        return "input";
    }

    List<GenTableColumn> buildColumnsFromModelRef(LowcodePageModelRef ref) {
        if (ref.getFields() == null || ref.getFields().isEmpty()) {
            return new ArrayList<>();
        }
        List<GenTableColumn> columns = new ArrayList<>();
        int sort = 0;
        for (Map<String, Object> fieldMap : ref.getFields()) {
            String sourceField = StringUtils.firstNonBlank(configuration.text(fieldMap.get("sourceField")),
                    configuration.text(fieldMap.get("field")), configuration.text(fieldMap.get("fieldRef")));
            if (StringUtils.isBlank(sourceField)) {
                continue;
            }
            GenTableColumn column = referencedColumn(fieldMap, sourceField, sort++);
            columns.add(column);
        }
        return columns;
    }

    static String toPascalCase(String key) {
        if (StringUtils.isBlank(key)) return key;
        String[] parts = key.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    String resolveColumnName(List<GenTableColumn> columns, String javaField) {
        if (StringUtils.isBlank(javaField) || columns == null) {
            return camelToSnake(javaField);
        }
        return columns.stream()
                .filter(column -> StringUtils.equals(javaField, column.getJavaField())
                        || StringUtils.equals(javaField, column.getColumnName()))
                .map(GenTableColumn::getColumnName)
                .findFirst()
                .orElse(camelToSnake(javaField));
    }

    static boolean hasColumn(List<GenTableColumn> columns, String columnName) {
        return columns != null && columns.stream()
                .anyMatch(column -> StringUtils.equalsIgnoreCase(columnName, column.getColumnName()));
    }

    static String capJavaField(String javaField) {
        String field = normalizeJavaField(javaField);
        if (StringUtils.isBlank(field)) {
            return field;
        }
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }

    static String normalizeJavaField(String field) {
        if (StringUtils.isBlank(field)) {
            return field;
        }
        String trimmed = field.trim();
        if (trimmed.contains("_") || trimmed.contains("-")) {
            return lowerCamel(trimmed);
        }
        return trimmed;
    }

    static String lowerCamel(String value) {
        String pascal = toPascalCase(value);
        if (StringUtils.isBlank(pascal)) {
            return pascal;
        }
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    static String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String normalized = value.replace("-", "_");
        return normalized
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

    private GenTableColumn modelColumn(LowcodeFieldSchema field, int sort) {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(field.getColumnName());
        column.setColumnComment(StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        column.setColumnType(toColumnType(field));
        column.setJavaType(toJavaType(field.getDataType()));
        column.setJavaField(StringUtils.defaultIfBlank(field.getField(), field.getColumnName()));
        column.setIsPk(Boolean.TRUE.equals(field.getPrimaryKey()) ? 1 : 0);
        column.setIsIncrement(Boolean.TRUE.equals(field.getAutoIncrement()) ? 1 : 0);
        column.setIsRequired(Boolean.TRUE.equals(field.getRequired()) ? 1 : 0);
        column.setIsInsert(Boolean.TRUE.equals(field.getFormVisible()) && !Boolean.TRUE.equals(field.getReadonly()) ? 1 : 0);
        column.setIsEdit(Boolean.TRUE.equals(field.getFormVisible()) && !Boolean.TRUE.equals(field.getReadonly()) ? 1 : 0);
        column.setIsList(field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()) ? 1 : 0);
        column.setIsQuery(Boolean.TRUE.equals(field.getSearchable()) ? 1 : 0);
        column.setQueryType(StringUtils.defaultIfBlank(field.getQueryType(), "EQ").toUpperCase(Locale.ROOT));
        column.setHtmlType(toHtmlType(field.getComponentType(), field.getDataType()));
        column.setDictType(StringUtils.trimToNull(field.getDictType()));
        column.setDesensitizeType(StringUtils.trimToNull(field.getSensitiveType()));
        column.setSort(sort);
        return column;
    }

    private GenTableColumn referencedColumn(Map<String, Object> fieldMap, String sourceField, int sort) {
        String columnName = StringUtils.defaultIfBlank(configuration.text(fieldMap.get("columnName")), camelToSnake(sourceField));
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(columnName);
        column.setColumnComment(StringUtils.firstNonBlank(configuration.text(fieldMap.get("rawLabel")),
                configuration.text(fieldMap.get("label")), sourceField));
        LowcodeFieldSchema lowcodeField = new LowcodeFieldSchema();
        lowcodeField.setDataType(configuration.text(fieldMap.get("dataType")));
        lowcodeField.setLength(configuration.integerValue(fieldMap.get("length")));
        lowcodeField.setPrecision(configuration.integerValue(fieldMap.get("precision")));
        column.setColumnType(toColumnType(lowcodeField));
        column.setJavaType(toJavaType(configuration.text(fieldMap.get("dataType"))));
        column.setJavaField(sourceField);
        boolean primaryKey = configuration.booleanValue(fieldMap.get("primaryKey")) || "id".equals(sourceField) || "id".equals(columnName);
        boolean readonly = configuration.booleanValue(fieldMap.get("readonly"));
        column.setIsPk(primaryKey ? 1 : 0);
        column.setIsIncrement(configuration.booleanValue(fieldMap.get("autoIncrement")) || primaryKey ? 1 : 0);
        column.setIsRequired(configuration.booleanValue(fieldMap.get("required")) ? 1 : 0);
        column.setIsInsert(!primaryKey && !readonly ? 1 : 0);
        column.setIsEdit(!primaryKey && !readonly ? 1 : 0);
        column.setIsList(configuration.booleanValueDefault(fieldMap.get("listVisible"), true) ? 1 : 0);
        column.setIsQuery(configuration.booleanValue(fieldMap.get("searchable")) ? 1 : 0);
        column.setQueryType(StringUtils.defaultIfBlank(configuration.text(fieldMap.get("queryType")), "EQ").toUpperCase(Locale.ROOT));
        column.setHtmlType(toHtmlType(configuration.text(fieldMap.get("componentType")), configuration.text(fieldMap.get("dataType"))));
        column.setDictType(StringUtils.trimToNull(configuration.text(fieldMap.get("dictType"))));
        column.setDesensitizeType(VelocityCodegenAnnotations.normalizeDesensitizeType(configuration.text(fieldMap.get("sensitiveType"))));
        column.setSort(sort);
        return column;
    }

    private Map<String, Map<String, Object>> translationFields(Map<String, Object> model) {

        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        if (model.get("fields") instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Map<?, ?> raw) {
                    Map<String, Object> field = new LinkedHashMap<>();
                    raw.forEach((key, item) -> field.put(String.valueOf(key), item));
                    fields.put(configuration.text(field.get("columnName")), field);
                }
            }
        }
        return fields;
    }
}
