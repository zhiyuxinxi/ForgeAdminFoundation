package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratedLowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Real generation snapshots captured before QUAL-02 extraction. Normal tests never update them. */
class VelocityCodegenStrategyRefactorTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> SCENARIOS = List.of("ordinary", "master-detail", "tree");

    @Test void ordinaryPackageRetainsCompleteOutput() throws Exception { verify("ordinary"); }
    @Test void masterDetailPackageRetainsCompleteOutput() throws Exception { verify("master-detail"); }
    @Test void treePackageRetainsCompleteOutput() throws Exception { verify("tree"); }

    @Test void contributorCannotOverwriteGeneratedFiles() throws Exception {
        LowcodeStaticCodegenContributor collision = new LowcodeStaticCodegenContributor() {
            @Override public String capabilityId() { return "collision"; }
            @Override public void contributeFiles(AiCrudConfig config, VelocityContext context,
                                                  Map<String, String> files) {
                files.put("sql/schema/biz_quality_record.sql", "replacement");
            }
        };
        BusinessException error = assertThrows(BusinessException.class,
                () -> strategy(collision).generate(fixture("ordinary"), null));
        assertTrue(error.getMessage().contains("collision"));
        assertTrue(error.getMessage().contains("sql/schema/biz_quality_record.sql"));
    }

    private void verify(String scenario) throws Exception {
        Map<String, String> files = generate(scenario);
        try (InputStream resource = getClass().getResourceAsStream("/qual02/" + scenario + "-sha256.json")) {
            assertNotNull(resource, "Frozen baseline must be captured explicitly before refactoring");
            Map<String, String> expected = JSON.readValue(resource, new TypeReference<>() { });
            assertEquals(expected, fingerprints(files), "Generated paths/content changed for " + scenario);
        }
    }

    /** Explicit operator capture entry point; intentionally unrelated to the JUnit test path. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"--capture".equals(args[0])) {
            throw new IllegalArgumentException("Expected --capture <new-baseline-directory>");
        }
        Path directory = Path.of(args[1]);
        Files.createDirectories(directory);
        for (String scenario : SCENARIOS) {
            Path output = directory.resolve(scenario + "-sha256.json");
            if (Files.exists(output)) throw new IllegalStateException("Refusing to overwrite " + output);
            Map<String, String> files = generate(scenario);
            Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(fingerprints(files)),
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
            System.out.println(scenario + ": " + files.size() + " files captured");
        }
    }

    private static Map<String, String> generate(String scenario) throws Exception {
        AiCrudConfig source = fixture(scenario);
        String original = JSON.writeValueAsString(source);
        Map<String, String> files = strategy(contributor()).generate(source, null);
        assertEquals(original, JSON.writeValueAsString(source), "Generation must not mutate saved config");
        assertCoverage(files, scenario);
        return files;
    }

    @SuppressWarnings("unchecked")
    private static VelocityCodegenStrategy strategy(LowcodeStaticCodegenContributor contributor) {
        GenTableColumnMapper mapper = mock(GenTableColumnMapper.class);
        when(mapper.selectDbTableColumnsByName(anyString())).thenReturn(List.of());
        when(mapper.selectConfiguredColumnsByTableName(anyString())).thenReturn(List.of());
        ObjectProvider<LowcodeStaticCodegenContributor> provider = mock(ObjectProvider.class);
        LowcodeStaticCodegenContributor unsupported = new LowcodeStaticCodegenContributor() {
            @Override public String capabilityId() { return "unsupported"; }
            @Override public boolean supports(AiCrudConfig config) { return false; }
            @Override public void contributeFiles(AiCrudConfig config, VelocityContext context,
                                                  Map<String, String> files) {
                fail("Unsupported contributor must not execute");
            }
        };
        when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(contributor, unsupported));
        return new VelocityCodegenStrategy(mapper, JSON, new GeneratedLowcodeRuntimeConfigBuilder(JSON),
                new LowcodeProtocolSnapshotBuilder(JSON), provider);
    }

    private static LowcodeStaticCodegenContributor contributor() {
        return new LowcodeStaticCodegenContributor() {
            @Override public String capabilityId() { return "quality-fixture"; }
            @Override public void contributeContext(AiCrudConfig config, VelocityContext context) {
                context.put("qualityMarker", "贡献者 / Contributor");
            }
            @Override public void contributeFiles(AiCrudConfig config, VelocityContext context,
                                                  Map<String, String> files) {
                assertTrue(config.getConfigKey().startsWith("generated_"));
                files.put("extensions/quality-fixture.txt", context.get("qualityMarker") + "\n"
                        + config.getConfigKey() + "\n" + context.get("staticCodegenContributors") + "\n");
            }
        };
    }

    private static AiCrudConfig fixture(String scenario) throws Exception {
        String layout = switch (scenario) {
            case "master-detail" -> "master-detail-crud";
            case "tree" -> "tree-crud";
            default -> "simple-crud";
        };
        Map<String, Object> status = field("status", "状态(1:启用,0:停用)", "Status", "int");
        status.put("dictType", "quality_status");
        status.put("dictLabelsI18n", map("1", map("en-US", "Enabled"), "0", map("en-US", "Disabled")));
        List<Map<String, Object>> fields = new ArrayList<>(List.of(
                field("id", "编号", "ID", "bigint"),
                field("recordName", "记录名称", "Record name", "varchar"), status,
                field("departmentId", "所属部门", "Department", "bigint"),
                field("delFlag", "删除标记", "Deleted", "bigint")));
        Map<String, Object> model = map("appType", "SINGLE", "tableMode", "EXISTING",
                "tableName", "biz_quality_record", "businessName", "质量记录",
                "businessNameI18n", map("en-US", "Owner's quality records"), "fields", fields);
        List<Map<String, Object>> refs = new ArrayList<>();
        refs.add(map("modelCode", "quality_record", "tableName", "biz_quality_record", "primary", true,
                "modelName", "质量记录", "fields", fields));
        Map<String, Object> options = map("codegen", map("includeBackend", true,
                "businessApiBase", "/quality/record", "packageName", "com.example.quality",
                "author", "QUAL-02 fixture", "functionNameEn", "Owner's quality records"));
        if ("master-detail".equals(scenario)) {
            refs.add(map("modelCode", "quality_detail", "tableName", "biz_quality_detail", "primary", false,
                    "modelName", "记录明细", "fields", List.of(field("id", "编号", "ID", "bigint"),
                            field("recordId", "主记录", "Record", "bigint"),
                            field("detailName", "明细名称", "Detail name", "varchar")),
                    "relations", List.of(map("relationType", "CHILD_LIST", "targetObjectCode", "quality_record",
                            "sourceField", "recordId", "targetField", "id"))));
        }
        if ("tree".equals(scenario)) {
            refs.add(map("modelCode", "quality_department", "tableName", "biz_quality_department", "primary", false,
                    "modelName", "部门", "fields", List.of(field("id", "编号", "ID", "bigint"),
                            field("parentId", "上级部门", "Parent", "bigint"),
                            field("name", "部门名称", "Department name", "varchar"))));
            options.put("treeConfig", map("sourceModelCode", "quality_department",
                    "sourceTableName", "biz_quality_department", "keyField", "id", "parentField", "parentId",
                    "labelField", "name", "filterField", "departmentId", "targetField", "id"));
        }
        Map<String, Object> page = map("layoutType", layout, "primaryModelCode", "quality_record", "modelRefs", refs,
                "zones", List.of(map("zoneKey", "table", "fieldRefs", List.of("recordName", "status")),
                        map("zoneKey", "edit", "fieldRefs", List.of("recordName", "status"))));
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("quality_record");
        config.setTableName("biz_quality_record");
        config.setTableComment("质量记录");
        config.setPrimaryKeyField("id");
        config.setBuildMode("LOWCODE");
        config.setLayoutType(layout);
        config.setModelSchema(JSON.writeValueAsString(model));
        config.setPageSchema(JSON.writeValueAsString(page));
        config.setOptions(JSON.writeValueAsString(options));
        config.setSearchSchema("[]");
        config.setColumnsSchema(JSON.writeValueAsString(List.of(
                map("key", "status", "title", "状态", "render", map("dictType", "quality_status")))));
        config.setEditSchema(JSON.writeValueAsString(fields));
        config.setApiConfig("{}");
        config.setDictConfig("[{\"dictType\":\"quality_status\",\"dictName\":\"状态\"}]");
        config.setTransConfig("{}");
        config.setDesensitizeConfig("{}");
        config.setEncryptConfig("{}");
        return config;
    }

    private static Map<String, Object> field(String name, String label, String english, String type) {
        return map("field", name, "columnName", name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT),
                "label", label, "labelI18n", map("en-US", english), "dataType", type, "length", 128,
                "primaryKey", "id".equals(name), "autoIncrement", "id".equals(name),
                "readonly", "id".equals(name), "formVisible", true, "listVisible", true,
                "searchable", false, "componentType", "varchar".equals(type) ? "input" : "inputNumber");
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static void assertCoverage(Map<String, String> files, String scenario) throws Exception {
        for (String suffix : List.of("Controller.java", "ServiceImpl.java", "Mapper.xml", "/index.vue",
                "_menu.sql", "_dict.sql", "_excel.sql", "/runtime-config.json", "-protocol.json",
                "-coverage.json", "-ownership.json", "/locales/zh-CN.js", "/locales/en-US.js")) {
            assertTrue(files.keySet().stream().anyMatch(path -> path.endsWith(suffix)), suffix);
        }
        String all = String.join("\n", files.values());
        assertTrue(all.contains("记录名称"));
        assertTrue(all.contains("Record name"));
        assertTrue(all.contains("Enabled"));
        assertTrue(all.contains("quality:record:edit"));
        assertTrue(all.contains("CREATE_ONCE_SAMPLE"));
        assertTrue(all.contains("NEVER_GENERATED_NEVER_OVERWRITTEN"));
        assertTrue(files.get("extensions/quality-fixture.txt").contains("贡献者 / Contributor"));
        String manifest = files.entrySet().stream().filter(entry -> entry.getKey().endsWith("-ownership.json"))
                .findFirst().orElseThrow().getValue();
        List<String> owned = new ArrayList<>();
        JSON.readTree(manifest).path("files").forEach(file -> owned.add(file.path("path").asText()));
        assertEquals(new ArrayList<>(new java.util.TreeSet<>(files.keySet())), owned);
        if ("master-detail".equals(scenario)) {
            assertTrue(all.contains("saveBizQualityDetailRows"));
            assertTrue(all.contains("record_id"));
        }
        if ("tree".equals(scenario)) {
            assertTrue(all.contains("selectTreeRows"));
            assertTrue(files.keySet().stream().anyMatch(path -> path.endsWith("BizQualityDepartmentMapper.xml")));
        }
    }

    private static Map<String, String> fingerprints(Map<String, String> files) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String normalized = entry.getValue().replace("\r\n", "\n")
                    .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}(?:[ T]\\d{2}:\\d{2}:\\d{2})?\\b", "<GENERATED_DATE>");
            if (entry.getKey().endsWith(".json")) {
                normalized = JSON.writeValueAsString(sortedJson(JSON.readTree(normalized)));
            }
            result.put(entry.getKey(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8))));
        }
        return result;
    }

    private static JsonNode sortedJson(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JSON.createObjectNode();
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((key, value) -> sorted.set(key, sortedJson(value)));
            return sorted;
        }
        if (node.isArray()) {
            var sorted = JSON.createArrayNode();
            node.forEach(value -> sorted.add(sortedJson(value)));
            return sorted;
        }
        return node;
    }
}
