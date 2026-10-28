package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCrudConfigServiceRuntimeRenderOptionsTest {
    private final LowcodeRuntimeConfigBuilder builder = mock(LowcodeRuntimeConfigBuilder.class);
    private final AiCrudConfigService service = new AiCrudConfigService(new ObjectMapper(), null, builder,
            mock(BusinessDocumentConfigService.class), null, mock(AiPageTemplateService.class));

    @Test
    void preservesExistingHelperCallAndNumericWidthCompatibility() {
        AiCrudConfigService.alignRuntimeOptionCompatibilityFields(null);
        AiCrudConfig config = config();
        config.setOptions("{\"modalWidth\":960,\"editGridCols\":2}");
        assertLayout(service.buildRenderConfig(config), "960", 2);
    }

    @Test
    void legacyOptionsReachTopLevelRuntimeProps() {
        AiCrudConfig config = config();
        config.setOptions("{\"modalWidth\":\"960px\",\"editGridCols\":2}");
        assertLayout(service.buildRenderConfig(config), "960px", 2);
    }

    @Test
    void publishedOptionsReachTopLevelRuntimeProps() {
        AiCrudConfig config = config();
        config.setModelSchema("{}");
        config.setPageSchema("{}");
        config.setOptions("{\"modalWidth\":\"90vw\",\"editGridCols\":\"3\"}");
        assertLayout(service.buildRenderConfig(config), "90vw", 3);
    }

    @Test
    void draftUsesCompiledOptionsRatherThanStalePublishedValues() {
        AiCrudConfig config = config();
        config.setModelSchema("{}");
        config.setPageSchema("{}");
        config.setOptions("{\"modalWidth\":\"720px\",\"editGridCols\":1}");
        LowcodeRuntimeConfig runtime = new LowcodeRuntimeConfig();
        runtime.setSearchSchema("[]");
        runtime.setColumnsSchema("[]");
        runtime.setEditSchema("[]");
        runtime.setApiConfig("{}");
        runtime.setOptions("{\"modalWidth\":\"1080px\",\"editGridCols\":2}");
        runtime.setDictConfig("{}");
        runtime.setDesensitizeConfig("{}");
        runtime.setEncryptConfig("{}");
        runtime.setTransConfig("{}");
        when(builder.buildRuntimeConfig(eq("sample"), any(), any())).thenReturn(runtime);
        assertLayout(service.buildDraftRenderConfig(config), "1080px", 2);
    }

    @Test
    void missingBlankOrInvalidOptionsKeepExistingDefaults() {
        for (String options : new String[]{null, "{}", "[]",
                "{\"modalWidth\":\"  \",\"editGridCols\":0}",
                "{\"editGridCols\":-1}", "{\"editGridCols\":1.5}", "{\"editGridCols\":\"invalid\"}"}) {
            AiCrudConfig config = config();
            config.setOptions(options);
            assertLayout(service.buildRenderConfig(config), "800px", 1);
        }
    }

    private AiCrudConfig config() {
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("sample");
        config.setSearchSchema("[]");
        config.setColumnsSchema("[]");
        config.setEditSchema("[]");
        config.setApiConfig("{}");
        return config;
    }

    private void assertLayout(AiCrudConfigRenderVO result, String width, int columns) {
        assertEquals(width, result.getModalWidth());
        assertEquals(columns, result.getEditGridCols());
    }
}
