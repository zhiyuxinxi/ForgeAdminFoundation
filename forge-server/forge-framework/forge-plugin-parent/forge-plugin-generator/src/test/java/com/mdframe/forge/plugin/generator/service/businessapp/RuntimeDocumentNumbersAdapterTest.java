package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.document.RuntimeDocumentNumbersAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuntimeDocumentNumbersAdapterTest {
    @Test
    void selectedDefinitionIsAnIndependentSnapshotAndGenerationKeepsTenantPayloadAndFailure() {
        BusinessDocumentConfigMapper definitions = mock(BusinessDocumentConfigMapper.class);
        ISequenceService sequences = mock(ISequenceService.class);
        BusinessDocumentConfigService service = new BusinessDocumentConfigService(definitions,
                mock(BusinessBindingMapper.class), mock(BusinessObjectService.class), mock(AiCrudConfigMapper.class),
                new ObjectMapper(), sequences);
        RuntimeDocumentNumbersAdapter adapter = new RuntimeDocumentNumbersAdapter(service);
        LowcodeRuntimeConfig runtime = new LowcodeRuntimeConfig();
        runtime.setTenantId(7L);
        runtime.setConfigKey("records");
        runtime.setObjectCode("OBJECT");
        AiBusinessDocumentConfig design = new AiBusinessDocumentConfig();
        design.setTenantId(42L);
        design.setSuiteCode("SUITE");
        design.setObjectCode("OBJECT");
        design.setDocumentEnabled(1);
        design.setDocumentNoRule("D-{seq4}-${field:title}");
        design.setOptions("{\"documentNoField\":\"record_code\"}");
        when(definitions.selectByObjectCode(7L, "OBJECT")).thenReturn(design);
        when(sequences.nextId(anyString())).thenReturn(17L);

        RuntimeDocumentNumber number = adapter.resolve(runtime);
        assertEquals(42L, number.tenantId());
        assertEquals("record_code", number.documentNoField());
        assertEquals("D-{seq4}-${field:title}", number.rawNoRule());
        design.setTenantId(99L);
        design.setDocumentNoRule("changed after lookup");
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("title", "sample", "tenantId", 88L));
        Map<String, Object> originalPayload = new LinkedHashMap<>(payload);
        assertEquals("D-0017-sample", adapter.generate(number, payload));
        String key = "lowcode:document-no:42:SUITE:OBJECT:all:"
                + Integer.toHexString("D-${seq:4}-${field:title}".hashCode());
        verify(sequences).nextId(key);
        assertEquals(originalPayload, payload);
        var lookup = inOrder(definitions);
        lookup.verify(definitions).selectByConfigKey(7L, "records");
        lookup.verify(definitions).selectByObjectCode(7L, "OBJECT");
        lookup.verifyNoMoreInteractions();

        BusinessException failure = new BusinessException("sequence unavailable");
        when(sequences.nextId(key)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> adapter.generate(number, payload)));
        verify(sequences, times(2)).nextId(key);
        verifyNoMoreInteractions(definitions);
        assertTrue(RuntimeDocumentNumber.class.getProtectionDomain().getCodeSource().getLocation()
                .toString().endsWith("forge-lowcode-api-numbering.jar"));
    }
}
