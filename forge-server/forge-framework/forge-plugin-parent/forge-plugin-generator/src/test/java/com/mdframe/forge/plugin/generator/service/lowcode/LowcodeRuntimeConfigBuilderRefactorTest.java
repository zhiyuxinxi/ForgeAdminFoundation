package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Fixed outputs captured from the original builder before responsibility extraction. */
class LowcodeRuntimeConfigBuilderRefactorTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @TestFactory
    List<DynamicTest> preservesPreRefactorRuntimeAndNormalization() throws Exception {
        JsonNode cases;
        try (InputStream source = getClass().getResourceAsStream("/qual02/lowcode-runtime-snapshots.json")) {
            assertNotNull(source);
            cases = mapper.readTree(source);
        }
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode fixture : cases) {
            String name = tests.size() + ": " + fixture.get("configKey").asText()
                    + " / " + fixture.get("page").get("layoutType").asText();
            tests.add(DynamicTest.dynamicTest(name, () -> assertSnapshot(fixture)));
        }
        return tests;
    }

    private void assertSnapshot(JsonNode fixture) throws Exception {
        LowcodeModelSchema model = mapper.treeToValue(fixture.get("model"), LowcodeModelSchema.class);
        LowcodePageSchema page = mapper.treeToValue(fixture.get("page"), LowcodePageSchema.class);
        LowcodeRuntimeConfigBuilder builder = new LowcodeRuntimeConfigBuilder(mapper,
                new LowcodeSchemaValidator(), new LowcodePolicyService());
        var runtime = builder.buildRuntimeConfig(fixture.get("configKey").asText(), model, page);
        assertEquals(fixture.get("expected"), mapper.valueToTree(runtime), "complete runtime contract");
        assertEquals(fixture.get("normalizedModel"), mapper.valueToTree(model), "model normalization");
        assertEquals(fixture.get("normalizedPage"), mapper.valueToTree(page), "page normalization");
    }
}
