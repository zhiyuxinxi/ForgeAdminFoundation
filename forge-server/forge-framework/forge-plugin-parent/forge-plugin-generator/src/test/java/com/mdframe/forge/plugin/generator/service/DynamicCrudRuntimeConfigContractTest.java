package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** The public compatibility entry retains published and preview admission semantics. */
class DynamicCrudRuntimeConfigContractTest {
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);

    private DynamicCrudService service() {
        ObjectMapper mapper = new ObjectMapper();
        return new DynamicCrudService(null, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), null, null, null, null, null, null, null);
    }

    private AiCrudConfig config(String status) {
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("suite_entry");
        config.setTenantId(7L);
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus(status);
        return config;
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publishedEntryKeepsOneReadAndTheSelectedSnapshot() {
        AiCrudConfig design = config("PUBLISHED");
        design.setModelSchema("draft bytes");
        AiCrudConfig published = config("PUBLISHED");
        published.setModelSchema(" { published: raw bytes } ");
        when(configs.getByConfigKey("suite_entry")).thenReturn(design);
        when(configs.resolvePublishedRuntimeConfig(design)).thenReturn(published);
        assertSame(published, service().getRuntimeConfig("suite_entry"));
        verify(configs).getByConfigKey("suite_entry");
        verify(configs).resolvePublishedRuntimeConfig(design);
    }

    @Test
    void previewFlagRequiresServerSidePermission() {
        AiCrudConfig draft = config("DRAFT");
        when(configs.getByConfigKey("suite_entry")).thenReturn(draft);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("designPreview", "1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThrows(BusinessException.class, () -> service().getRuntimeConfig("suite_entry"));
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
    }

    @Test
    void authorizedPreviewUsesCurrentDraftWithoutPublishedLookup() {
        AiCrudConfig draft = config("DRAFT");
        when(configs.getByConfigKey("suite_entry")).thenReturn(draft);
        when(configs.hasDesignPreviewPermission()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("designPreview", "1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertSame(draft, service().getRuntimeConfig("suite_entry"));
        verify(configs).getByConfigKey("suite_entry");
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
    }

    @Test
    void disabledAndWrongModeRemainRejectedBeforeSnapshotResolution() {
        AiCrudConfig disabled = config("PUBLISHED");
        disabled.setStatus("1");
        AiCrudConfig wrongMode = config("PUBLISHED");
        wrongMode.setMode("CODE");
        when(configs.getByConfigKey("suite_entry")).thenReturn(disabled, wrongMode);
        DynamicCrudService service = service();
        assertThrows(BusinessException.class, () -> service.getRuntimeConfig("suite_entry"));
        assertThrows(BusinessException.class, () -> service.getRuntimeConfig("suite_entry"));
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
    }

    @Test
    void runtimeValuesCopyEveryFieldWithoutParsingOrSharingMutation() throws ReflectiveOperationException {
        AiCrudConfig source = config("PUBLISHED");
        for (var field : LowcodeRuntimeConfig.class.getDeclaredFields()) {
            String suffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            Object value = field.getType() == Long.class ? 37L : "  { raw-invalid-" + field.getName() + " }  ";
            AiCrudConfig.class.getMethod("set" + suffix, field.getType()).invoke(source, value);
        }
        LowcodeRuntimeConfig first = RuntimeCrudConfigAdapter.fromDesign(source);
        LowcodeRuntimeConfig second = RuntimeCrudConfigAdapter.fromDesign(source);
        assertEquals(31, LowcodeRuntimeConfig.class.getDeclaredFields().length);
        assertNotSame(first, second);
        for (var field : LowcodeRuntimeConfig.class.getDeclaredFields()) {
            String suffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            assertEquals(AiCrudConfig.class.getMethod("get" + suffix).invoke(source),
                    LowcodeRuntimeConfig.class.getMethod("get" + suffix).invoke(first), field.getName());
        }
        first.setTableName("effective_runtime_table");
        assertEquals(source.getTableName(), second.getTableName());
        assertNotEquals(first.getTableName(), source.getTableName());
        assertTrue(LowcodeRuntimeConfig.class.getProtectionDomain().getCodeSource().getLocation()
                .toString().endsWith("forge-lowcode-api-runtime-config.jar"));
    }

    @Test
    void runtimeEntryCopiesTheSelectedPublishedSnapshotWithOneRead() {
        AiCrudConfig design = config("PUBLISHED");
        AiCrudConfig published = config("PUBLISHED");
        published.setTableName("published_table");
        published.setModelSchema(" invalid JSON stays at the original consumer ");
        when(configs.getByConfigKey("suite_entry")).thenReturn(design);
        when(configs.resolvePublishedRuntimeConfig(design)).thenReturn(published);
        LowcodeRuntimeConfig values = service().getRuntimeValues("suite_entry");
        assertEquals(7L, values.getTenantId());
        assertEquals(published.getModelSchema(), values.getModelSchema());
        values.setTableName("effective_table");
        assertEquals("published_table", published.getTableName());
        verify(configs).getByConfigKey("suite_entry");
        verify(configs).resolvePublishedRuntimeConfig(design);
    }

    @Test
    void allowDraftCopiesCurrentValuesWithoutPublishedLookupOrJsonParsing() {
        AiCrudConfig draft = config("DRAFT");
        draft.setOptions(" malformed options ");
        when(configs.getByConfigKey("suite_entry")).thenReturn(draft);
        LowcodeRuntimeConfig values = new RuntimeCrudConfigAdapter(configs).loadAllowDraft("suite_entry");
        assertEquals("DRAFT", values.getPublishStatus());
        assertEquals(draft.getOptions(), values.getOptions());
        verify(configs).getByConfigKey("suite_entry");
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
        verify(configs, never()).hasDesignPreviewPermission();
    }

    @Test
    void runtimeEntryPropagatesSnapshotFailureUnchanged() {
        AiCrudConfig design = config("PUBLISHED");
        IllegalStateException failure = new IllegalStateException("snapshot unavailable");
        when(configs.getByConfigKey("suite_entry")).thenReturn(design);
        when(configs.resolvePublishedRuntimeConfig(design)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service().getRuntimeValues("suite_entry")));
    }

    @Test
    void publicAllowDraftReadUsesEffectiveTableAndLeavesDesignCopyUnchanged() {
        AiCrudConfig draft = config("DRAFT");
        draft.setTableName("design_table");
        when(configs.getByConfigKey("suite_entry")).thenReturn(draft);
        DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
        DynamicDataScopeService scopes = mock(DynamicDataScopeService.class);
        LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
        RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(any())).thenReturn(LowcodeRuntimeDataSourceContext.master("effective_table"));
        when(repository.selectById(eq("effective_table"), eq("id"), eq(8L), isNull()))
                .thenReturn(new java.util.LinkedHashMap<>(java.util.Map.of("id", 8L)));
        ObjectMapper mapper = new ObjectMapper();
        DynamicCrudService service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), scopes, null, null, null, null, null, sources);
        assertEquals(8L, service.selectByIdAllowDraft("suite_entry", 8L).get("id"));
        verify(scopes).buildRuntimeCondition(argThat(value -> value.getTenantId().equals(7L)
                && value.getTableName().equals("effective_table")), eq("effective_table"), isNull());
        assertEquals("design_table", draft.getTableName());
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }
}
