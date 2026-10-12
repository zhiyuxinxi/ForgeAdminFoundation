package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentNoRulePreviewDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessDocumentConfigurationContractTest {
    private final BusinessDocumentConfigMapper documents = mock(BusinessDocumentConfigMapper.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessObjectService objects = mock(BusinessObjectService.class);
    private final AiCrudConfigMapper schemas = mock(AiCrudConfigMapper.class);
    private final ISequenceService sequences = mock(ISequenceService.class);
    private final BusinessDocumentConfigService service = new BusinessDocumentConfigService(
            documents, bindings, objects, schemas, new ObjectMapper(), sequences);

    @Test
    void previewNormalizesLegacyTokensAndReadsFieldAliasesWithoutAllocatingSequence() {
        var request = new BusinessDocumentNoRulePreviewDTO();
        request.setTemplate("A-{seq4}-${field:customer_name}-${objectCode}");
        request.setSequence(12);
        request.setObjectCode("ORDER");
        request.setSampleData(Map.of("customerName", "ACME"));
        var result = service.previewNoRule(request);
        assertEquals("A-0012-ACME-ORDER", result.getPreviewNo());
        assertTrue(result.getValid());
        assertEquals(List.of("${seq:4}", "${field:customer_name}", "${objectCode}"), result.getUsedTokens());
        verifyNoInteractions(sequences);
    }

    @Test
    void previewDistinguishesInvalidTokensAndMissingSampleWarnings() {
        var request = new BusinessDocumentNoRulePreviewDTO();
        request.setTemplate("${unknown}-${seq:no}-${field:missing}");
        var result = service.previewNoRule(request);
        assertFalse(result.getValid());
        assertEquals(2, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
        assertEquals("--missing", result.getPreviewNo());
    }

    @Test
    void numberAllocationIsScopedToTenantObjectAndNormalizedTemplate() {
        var config = document();
        config.setDocumentNoRule("D-{seq4}");
        when(sequences.nextId(anyString())).thenReturn(17L);
        assertEquals("D-0017", service.generateDocumentNo(config, Map.of()));
        String hash = Integer.toHexString("D-${seq:4}".hashCode());
        verify(sequences).nextId("lowcode:document-no:7:SUITE:ORDER:all:" + hash);
        config.setDocumentEnabled(0);
        assertNull(service.generateDocumentNo(config, Map.of()));
        verifyNoMoreInteractions(sequences);
    }

    @Test
    void explicitNumberFieldWinsAndInferenceUsesScoreThenStableOrder() {
        var config = document();
        var schema = new AiCrudConfig();
        schema.setModelSchema("{\"fields\":[{\"field\":\"applyNo\"},{\"field\":\"documentNo\"}]}");
        schema.setEditSchema("[{\"field\":\"other\",\"label\":\"单据编号\"}]");
        assertEquals("documentNo", service.resolveDocumentNoField(config, schema));
        config.setOptions("{\"noField\":\" explicit_no \"}");
        assertEquals("explicit_no", service.resolveDocumentNoField(config, schema));
        verifyNoInteractions(schemas);
    }

    @Test
    void malformedModelFallsBackToEditSchemaAndWeakCandidatesAreRejected() {
        var schema = new AiCrudConfig();
        schema.setModelSchema("broken");
        schema.setEditSchema("[{\"prop\":\"billNo\"}]");
        assertEquals("billNo", service.resolveDocumentNoField(document(), schema));
        schema.setEditSchema("[{\"field\":\"phoneNumber\"}]");
        assertNull(service.resolveDocumentNoField(document(), schema));
    }

    @Test
    void enabledApprovalBindingWinsOverDisabledFlowAndKeepsProjection() {
        var disabled = new AiBusinessBinding();
        disabled.setStatus(0);
        var approval = new AiBusinessBinding();
        approval.setId(42L);
        approval.setBindingType("APPROVAL");
        approval.setBindingKey("legacy");
        approval.setStatus(1);
        approval.setBindingConfig("{\"flowKey\":\"review\",\"startMode\":\"AUTO\",\"variableMapping\":[{}]}");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "ORDER", "FLOW")).thenReturn(disabled);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "ORDER", "APPROVAL")).thenReturn(approval);
        var result = service.toVO(document(), new AiCrudConfig());
        assertEquals("review", result.getDefaultFlowKey());
        assertEquals("TRIGGER", result.getMainFlowSummary().get("startMode"));
        assertEquals(true, result.getMainFlowSummary().get("complete"));
        assertEquals("LEGACY_APPROVAL_BINDING", result.getMainFlowSummary().get("compatibilitySource"));
        assertEquals(7, result.getStatusMappingRows().size());
    }

    @Test
    void savePreservesTrustedTenantFieldAliasesStatusAndTransactionBoundary() throws Exception {
        var object = new AiBusinessObject();
        object.setId(9L);
        object.setObjectCode("ORDER");
        object.setObjectName("Order");
        object.setConfigKey("order");
        when(objects.requireEntity(9L)).thenReturn(object);
        var schema = new AiCrudConfig();
        schema.setModelSchema("{\"fields\":[{\"field\":\"order_status\"},{\"field\":\"document_no\"}]}");
        when(schemas.selectByConfigKey(7L, "order")).thenReturn(schema);
        var request = new BusinessDocumentConfigDTO();
        request.setDocumentEnabled(true);
        request.setStatusField("orderStatus");
        request.setNoRuleTemplate("O-{seq4}");
        request.setStatusMapping(Map.of("DRAFT", "NEW"));
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            service.saveConfig(9L, request);
        }
        var saved = ArgumentCaptor.forClass(AiBusinessDocumentConfig.class);
        verify(documents).insert(saved.capture());
        assertEquals(7L, saved.getValue().getTenantId());
        assertEquals("O-${seq:4}", saved.getValue().getDocumentNoRule());
        assertEquals("NEW", new ObjectMapper().readTree(saved.getValue().getStatusMapping()).get("DRAFT").asText());
        assertEquals("document_no", new ObjectMapper().readTree(saved.getValue().getOptions()).get("documentNoField").asText());
        var annotation = BusinessDocumentConfigService.class.getMethod("saveConfig", Long.class,
                BusinessDocumentConfigDTO.class).getAnnotation(Transactional.class);
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor());
    }

    @Test
    void sequenceFailurePropagatesWithoutIssuingAnotherNumber() {
        var config = document();
        config.setDocumentNoRule("${seq}");
        var failure = new BusinessException("sequence unavailable");
        when(sequences.nextId(anyString())).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> service.generateDocumentNo(config, Map.of())));
        verify(sequences, times(1)).nextId(anyString());
    }

    private AiBusinessDocumentConfig document() {
        var config = new AiBusinessDocumentConfig();
        config.setTenantId(7L);
        config.setDocumentEnabled(1);
        config.setSuiteCode("SUITE");
        config.setObjectCode("ORDER");
        return config;
    }

    @Test
    void missingTenantContextCannotFallBackToTenantOne() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(null);
            assertThrows(BusinessException.class, () -> service.selectEnabledByObjectCode("ORDER"));
        }
        verifyNoInteractions(documents);
    }

    @Test
    void inferenceBusinessFailureDoesNotBecomeACompatibilityFallback() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        var failure = new BusinessException("invalid schema policy");
        when(mapper.readValue(anyString(), eq(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema.class)))
                .thenThrow(failure);
        var target = new BusinessDocumentConfigService(documents, bindings, objects, schemas, mapper, sequences);
        var schema = new AiCrudConfig();
        schema.setModelSchema("{}");
        assertSame(failure, assertThrows(BusinessException.class, () -> target.resolveDocumentNoField(document(), schema)));
    }
}
