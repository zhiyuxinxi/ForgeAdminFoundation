package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.lowcode.api.binding.BusinessFlowBindingQuery;
import com.mdframe.forge.lowcode.api.code.BusinessCodeGenerator;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleGenerateVO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LowcodeRuntimePortContractTest {
    @Test
    void flowSelectionPreservesPrecedenceEvenWhenItsConfigIsBlank() {
        var mapper = mock(BusinessBindingMapper.class);
        var row = binding(4L, "");
        when(mapper.selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "FLOW")).thenReturn(row);
        BusinessFlowBindingQuery query = new GeneratorBusinessFlowBindingQuery(mapper);
        var value = query.findForObject(7L, "invoice").orElseThrow();
        assertEquals(4L, value.bindingId());
        assertEquals("", value.bindingConfig());
        verify(mapper, never()).selectBindingByTypeAndCode(anyLong(), anyString(), anyString(), eq("APPROVAL"));
    }

    @Test
    void approvalFallbackUsesTheSameTenantAndObjectAndDoesNotExposeEntity() {
        var mapper = mock(BusinessBindingMapper.class);
        when(mapper.selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "APPROVAL"))
                .thenReturn(binding(8L, "{\"titleTemplate\":\"Review\"}"));
        var value = new GeneratorBusinessFlowBindingQuery(mapper).findForObject(7L, "invoice").orElseThrow();
        assertEquals(8L, value.bindingId());
        assertEquals("{\"titleTemplate\":\"Review\"}", value.bindingConfig());
        var order = inOrder(mapper);
        order.verify(mapper).selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "FLOW");
        order.verify(mapper).selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "APPROVAL");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void absentBindingIsEmptyButPersistenceFailureIsNotAnEmptySuccess() {
        var mapper = mock(BusinessBindingMapper.class);
        var query = new GeneratorBusinessFlowBindingQuery(mapper);
        assertTrue(query.findForObject(7L, "invoice").isEmpty());
        var failure = new IllegalStateException("query failed");
        when(mapper.selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "FLOW")).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> query.findForObject(7L, "invoice")));
    }

    @Test
    void noTenantOrObjectNeverBecomesATenantOneQuery() {
        var mapper = mock(BusinessBindingMapper.class);
        var query = new GeneratorBusinessFlowBindingQuery(mapper);
        assertThrows(IllegalArgumentException.class, () -> query.findForObject(null, "invoice"));
        assertThrows(IllegalArgumentException.class, () -> query.findForObject(0L, "invoice"));
        assertThrows(IllegalArgumentException.class, () -> query.findForObject(7L, " "));
        verifyNoInteractions(mapper);
    }

    @Test
    void numberPortUsesTheExistingGenerationEntryAndPropagatesItsFailure() {
        var service = spy(new CodeRuleService(null, null, null));
        var result = new CodeRuleGenerateVO();
        result.setCode("INV-001");
        doReturn(result).when(service).generateResult(eq("invoice"), anyMap());
        BusinessCodeGenerator generator = service;
        assertEquals("INV-001", generator.generate("invoice", Map.of("amount", 2)));
        verify(service).generateResult("invoice", Map.of("amount", 2));
        var failure = new IllegalStateException("sequence unavailable");
        doThrow(failure).when(service).generateResult(eq("invoice"), anyMap());
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> generator.generate("invoice", Map.of("amount", 2))));
    }

    private static AiBusinessBinding binding(Long id, String config) {
        var value = new AiBusinessBinding();
        value.setId(id);
        value.setBindingConfig(config);
        return value;
    }
}
