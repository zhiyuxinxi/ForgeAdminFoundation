package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.*;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BilingualCodegenTest {
    @Test void structuredRuleValidationUsesLocaleWithoutChangingFieldsOperatorsOrValues() {
        var compiler = new com.mdframe.forge.plugin.generator.service.formula.ConditionRuleCompiler();
        var request = new com.mdframe.forge.plugin.generator.dto.formula.ConditionRuleCompileRequest();
        var rule = com.mdframe.forge.plugin.generator.domain.formula.ConditionRuleNode
                .condition("customerValue", "INVALID_OP", "客户原值-租户不存在");
        request.setRule(rule);
        try {
            org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.US);
            var english = compiler.compile(request);
            assertFalse(english.isValid());
            assertEquals(List.of("$ unsupported condition operator: INVALID_OP"), english.getErrors());
            org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertEquals(List.of("$ 不支持的条件操作符: INVALID_OP"), compiler.compile(request).getErrors());
            assertSame(rule, request.getRule());
            assertEquals("customerValue", rule.getField());
            assertEquals("INVALID_OP", rule.getOp());
            assertEquals("客户原值-租户不存在", rule.getValue());
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test void runtimeErrorsTranslateDeclaredFragmentsAndKeepBusinessArguments() {
        var codec = new com.mdframe.forge.plugin.generator.manager.coderule.CodeRuleRadixCodec();
        String original = "租户不存在-客户编码";
        try {
            org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.US);
            var error = assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                    () -> codec.alphabet(original, false));
            assertEquals("Unsupported sequence number base: " + original, error.getMessage());
            org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            var chinese = assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                    () -> codec.alphabet(original, false));
            assertEquals("不支持的流水号进制: " + original, chinese.getMessage());
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test void nativeTemplatesKeepEnglishMetadataAndEscapeDisplayValuesWithoutChangingPermissions() {
        GenTableColumn id = column("id", "编号", "ID");
        id.setJavaType("Long"); id.setIsPk(1); id.setIsInsert(0); id.setIsEdit(0);
        GenTableColumn status = column("status", "状态(1:启用,0:停用)", "Status");
        status.setDictType("example_status");
        status.setDictLabelsI18n(Map.of("1", Map.of("en-US", "Enabled"), "0", Map.of("en-US", "Disabled")));
        GenTable table = new GenTable();
        table.setTableName("example_record"); table.setClassName("Record"); table.setBusinessName("record");
        table.setFunctionName("记录"); table.setFunctionNameEn("Owner's records"); table.setModuleName("example");
        table.setPackageName("com.example"); table.setColumns(List.of(id, status)); table.setPkColumn(id);
        VelocityUtils.initVelocity();
        VelocityContext context = VelocityUtils.prepareContext(table);
        context.put("viewPath", "example/record"); context.put("businessApiBase", "/example/record");
        String menu = VelocityUtils.renderTemplate("templates/vm/sql/menu.sql.vm", context);
        String dictionary = VelocityUtils.renderTemplate("templates/vm/sql/dict.sql.vm", context);
        String excel = VelocityUtils.renderTemplate("templates/vm/sql/excel.sql.vm", context);
        assertTrue(menu.contains("resource_name_en"));
        assertTrue(menu.contains("Owner''s records"));
        assertTrue(menu.contains("example:record:list"));
        assertTrue(menu.contains("resource.del_flag = 0"));
        assertTrue(dictionary.contains("dict_label_en"));
        assertTrue(dictionary.contains("'Enabled', '1'"));
        assertTrue(dictionary.contains("'Disabled', '0'"));
        assertTrue(excel.contains("column_name_en"));
        assertTrue(excel.contains("'Status'"));
        assertFalse(menu.contains("$i18n"));
        assertFalse(dictionary.contains("$i18n"));
        assertFalse(excel.contains("$i18n"));
        context.put("functionNameEn", "Owner's \"records\"");
        String controller = VelocityUtils.renderTemplate("templates/vm/controller.java.vm", context);
        String service = VelocityUtils.renderTemplate("templates/vm/serviceImpl.java.vm", context);
        assertTrue(controller.contains("UiMessages.message(\"codegen.importTemplateFilename\""));
        assertTrue(controller.contains("UiMessages.label(\"记录\", \"Owner's \\\"records\\\"\")"));
        assertTrue(service.contains("UiMessages.message(\"codegen.importFailure\""));
        assertFalse(controller.contains("$i18n"));
        assertFalse(service.contains("$i18n"));
        List<?> form = (List<?>) context.get("editSchema");
        assertEquals("Status", ((Map<?, ?>)((Map<?, ?>)form.get(0)).get("labelI18n")).get("en-US"));
    }

    @Test void generatedBuiltInActionsUseExactlyTheMenuPermissions() throws Exception {
        ObjectMapper json = new ObjectMapper();
        LowcodeFieldSchema id = field("id", "编号", "ID");
        id.setDataType("bigint"); id.setPrimaryKey(true); id.setAutoIncrement(true); id.setReadonly(true);
        LowcodeFieldSchema name = field("recordName", "记录名称", "Record name");
        name.setColumnName("record_name"); name.setRequired(true); name.setSearchable(true);
        name.setPlaceholderI18n(Map.of("en-US", "Enter a record name"));
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setAppType("SINGLE"); model.setTableMode("EXISTING"); model.setTableName("example_record");
        model.setBusinessName("记录"); model.setBusinessNameI18n(Map.of("en-US", "Records"));
        model.setFields(List.of(id, name));
        LowcodePageSchema page = new LowcodePageSchema(); page.setLayoutType("simple-crud");
        List<LowcodePageZone> zones = new ArrayList<>();
        for (String key : List.of("search", "table", "edit")) {
            LowcodePageZone zone = new LowcodePageZone(); zone.setZoneKey(key);
            zone.setFieldRefs(List.of("recordName")); zones.add(zone);
        }
        page.setZones(zones);
        LowcodeRuntimeConfig runtime = new LowcodeRuntimeConfigBuilder(json, new LowcodeSchemaValidator(), new LowcodePolicyService())
                .buildRuntimeConfig("example_record", model, page);
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("example_record"); config.setTableName("example_record"); config.setTableComment("记录");
        config.setBuildMode("LOWCODE"); config.setPrimaryKeyField("id"); config.setLayoutType("simple-crud");
        config.setModelSchema(json.writeValueAsString(model)); config.setPageSchema(json.writeValueAsString(page));
        config.setSearchSchema(runtime.getSearchSchema()); config.setColumnsSchema(runtime.getColumnsSchema());
        config.setEditSchema(runtime.getEditSchema()); config.setApiConfig(runtime.getApiConfig());
        config.setDictConfig("[]"); config.setTransConfig("{}"); config.setDesensitizeConfig("{}"); config.setEncryptConfig("{}");
        config.setOptions("{\"codegen\":{\"includeBackend\":false,\"businessApiBase\":\"/example/record\"}}");
        GenTableColumnMapper columns = mock(GenTableColumnMapper.class);
        when(columns.selectDbTableColumnsByName("example_record")).thenReturn(List.of());
        when(columns.selectConfiguredColumnsByTableName("example_record")).thenReturn(List.of());
        VelocityCodegenStrategy strategy = new VelocityCodegenStrategy(columns, json,
                new GeneratedLowcodeRuntimeConfigBuilder(json), new LowcodeProtocolSnapshotBuilder(json), null);
        String originalOptions = config.getOptions();
        Map<String, String> files = strategy.generate(config, null);
        var runtimeDocument = json.readTree(files.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/runtime-config.json")).findFirst().orElseThrow().getValue());
        var actionPermissions = runtimeDocument.path("options").path("actionPermissions");
        assertEquals(7, actionPermissions.size());
        Map<String, String> suffixes = Map.of("add", "add", "detail", "query", "edit", "edit",
                "delete", "remove", "batchDelete", "remove", "import", "add", "export", "query");
        String menu = files.entrySet().stream().filter(e -> e.getKey().endsWith("menu.sql"))
                .findFirst().orElseThrow().getValue();
        suffixes.forEach((key, suffix) -> {
            String permission = "example:record:" + suffix;
            assertEquals(permission, actionPermissions.path(key).asText());
            assertTrue(menu.contains("'" + permission + "'"), key + " must match the generated menu");
        });
        assertTrue(runtimeDocument.toString().contains("Record name"));
        assertEquals(originalOptions, config.getOptions(), "Generation must not retrofit saved legacy configurations");
        assertTrue(files.entrySet().stream().anyMatch(e -> e.getKey().endsWith("/index.vue")
                && e.getValue().contains("LowcodeRuntimePage") && e.getValue().contains("runtimeConfig")));
    }

    @Test void modelToRuntimeToActualGeneratedPackageRetainsBothLanguagesAndBusinessIdentifiers() throws Exception {
        ObjectMapper json = new ObjectMapper();
        LowcodeFieldSchema id = field("id", "编号", "ID");
        id.setDataType("bigint"); id.setPrimaryKey(true); id.setAutoIncrement(true); id.setReadonly(true);
        LowcodeFieldSchema name = field("recordName", "记录名称", "Record name");
        name.setColumnName("record_name"); name.setRequired(true); name.setSearchable(true);
        name.setPlaceholderI18n(Map.of("en-US", "Enter a record name"));
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setAppType("SINGLE"); model.setTableMode("EXISTING"); model.setTableName("example_record");
        model.setBusinessName("记录"); model.setBusinessNameI18n(Map.of("en-US", "Records"));
        model.setFields(List.of(id, name));
        LowcodePageSchema page = new LowcodePageSchema(); page.setLayoutType("simple-crud");
        List<LowcodePageZone> zones = new ArrayList<>();
        for (String key : List.of("search", "table", "edit")) {
            LowcodePageZone zone = new LowcodePageZone(); zone.setZoneKey(key);
            zone.setFieldRefs(List.of("recordName")); zones.add(zone);
        }
        page.setZones(zones);
        LowcodeRuntimeConfig runtime = new LowcodeRuntimeConfigBuilder(json, new LowcodeSchemaValidator(), new LowcodePolicyService())
                .buildRuntimeConfig("example_record", model, page);
        assertTrue(runtime.getSearchSchema().contains("Record name"));
        assertTrue(runtime.getColumnsSchema().contains("Record name"));
        assertTrue(runtime.getEditSchema().contains("Enter a record name"));
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("example_record"); config.setTableName("example_record"); config.setTableComment("记录");
        config.setBuildMode("LOWCODE"); config.setPrimaryKeyField("id"); config.setLayoutType("simple-crud");
        config.setModelSchema(json.writeValueAsString(model)); config.setPageSchema(json.writeValueAsString(page));
        config.setSearchSchema(runtime.getSearchSchema()); config.setColumnsSchema(runtime.getColumnsSchema());
        config.setEditSchema(runtime.getEditSchema()); config.setApiConfig(runtime.getApiConfig());
        config.setDictConfig("[]"); config.setTransConfig("{}"); config.setDesensitizeConfig("{}"); config.setEncryptConfig("{}");
        config.setOptions("{\"codegen\":{\"includeBackend\":false,\"businessApiBase\":\"/example/record\"}}");
        GenTableColumnMapper columns = mock(GenTableColumnMapper.class);
        when(columns.selectDbTableColumnsByName("example_record")).thenReturn(List.of());
        when(columns.selectConfiguredColumnsByTableName("example_record")).thenReturn(List.of());
        VelocityCodegenStrategy strategy = new VelocityCodegenStrategy(columns, json,
                new GeneratedLowcodeRuntimeConfigBuilder(json), new LowcodeProtocolSnapshotBuilder(json), null);
        Map<String, String> files = strategy.generate(config, null);
        String runtimeJson = files.entrySet().stream().filter(e -> e.getKey().endsWith("/runtime-config.json")).findFirst().orElseThrow().getValue();
        assertTrue(runtimeJson.contains("Record name"));
        assertTrue(runtimeJson.contains("record_name"));
        assertTrue(runtimeJson.contains("记录名称"));
        assertTrue(files.entrySet().stream().anyMatch(e -> e.getKey().endsWith("/locales/en-US.js") && e.getValue().contains("Records")));
        assertTrue(files.entrySet().stream().anyMatch(e -> e.getKey().endsWith("/locales/zh-CN.js") && e.getValue().contains("记录名称")));
        assertTrue(files.entrySet().stream().anyMatch(e -> e.getKey().endsWith("/index.vue") && e.getValue().contains("business.generated_")));
        assertTrue(files.values().stream().anyMatch(v -> v.contains("resource_name_en") && v.contains("'Records'")));
    }

    private static GenTableColumn column(String field, String label, String english) {
        GenTableColumn column = new GenTableColumn(); column.setColumnName(field); column.setJavaField(field);
        column.setColumnComment(label); column.setColumnCommentEn(english); column.setJavaType("String");
        column.setColumnType("varchar(255)"); column.setIsPk(0); column.setIsList(1);
        column.setIsInsert(1); column.setIsEdit(1); column.setIsQuery(1); column.setIsRequired(0);
        return column;
    }
    private static LowcodeFieldSchema field(String key, String chinese, String english) {
        LowcodeFieldSchema field = new LowcodeFieldSchema(); field.setField(key); field.setColumnName(key);
        field.setLabel(chinese); field.setLabelI18n(Map.of("en-US", english)); field.setDataType("varchar");
        field.setLength(255); field.setFormVisible(true); field.setListVisible(true); field.setComponentType("input");
        return field;
    }
}
