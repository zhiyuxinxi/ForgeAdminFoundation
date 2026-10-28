package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFieldSchemaService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessNamingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostFieldGenerationTest {
    @Test
    void explicitPostFieldGeneratesStableIdAndSeparateDisplaySnapshot() throws Exception {
        BusinessFieldDTO dto = new BusinessFieldDTO();
        dto.setFieldName("负责岗位");
        dto.setFieldCode("postId");
        dto.setFieldType("POST");
        LowcodeFieldSchema field = new BusinessFieldSchemaService(
                new LowcodeModelSchemaNormalizer(), new BusinessNamingService()).buildFieldSchema(dto);
        assertThat(field.getBusinessFieldType()).isEqualTo("POST");
        assertThat(field.getDataType()).isEqualTo("bigint");
        assertThat(field.getComponentType()).isEqualTo("postSelect");
        field.setFormVisible(true);
        field.setBasicProps(Map.of("labelValueField", "postName"));
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setAppType("SINGLE");
        model.setTableMode("EXISTING");
        model.setTableName("test_post_reference");
        model.setBusinessName("岗位引用");
        model.setFields(List.of(field));
        LowcodePageSchema page = new LowcodePageSchema();
        page.setLayoutType("simple-crud");
        ObjectMapper json = new ObjectMapper();
        var runtime = new LowcodeRuntimeConfigBuilder(json, new LowcodeSchemaValidator(), new LowcodePolicyService())
                .buildRuntimeConfig("test_post_reference", model, page);
        JsonNode edit = json.readTree(runtime.getEditSchema());
        JsonNode generated = edit.findParents("field").stream()
                .filter(node -> "postId".equals(node.path("field").asText())).findFirst().orElseThrow();
        assertThat(generated.path("type").asText()).isEqualTo("postSelect");
        assertThat(generated.path("props").path("labelValueField").asText()).isEqualTo("postName");
        assertThat(generated.path("props").path("targetField").asText()).isNotEqualTo("postId");
    }
}
