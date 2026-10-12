package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public service behavior baseline, shared unchanged by the extraction. */
class BusinessFlowReadViewsTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessFlowInstanceLinkMapper links = mock(BusinessFlowInstanceLinkMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private final DynamicCrudService records = mock(DynamicCrudService.class);
    private final BusinessCodeFormProviderRegistry providers = mock(BusinessCodeFormProviderRegistry.class);
    private BusinessFlowService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        service = new BusinessFlowService(bindings, links, configs, objects, documents,
                mock(BusinessDocumentRuntimeService.class), records,
                mock(BusinessFieldDesignService.class), mock(BusinessFlowVariableResolver.class),
                providers, mock(ApplicationEventPublisher.class), mock(ObjectProvider.class), mock(ObjectProvider.class));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void statusResolvesAliasInOrderAndUsesCurrentTenant() {
        aliasDocument();
        var status = service.getFlowStatus("alias", 21L);
        assertEquals("generic:21", status.getBusinessKey());
        assertEquals("NOT_STARTED", status.getFlowStatus());
        assertEquals("尚未发起主流程", status.getMessage());
        var order = inOrder(configs, documents, objects, links);
        order.verify(configs).selectPublishedByObjectCodeOrConfigKey(7L, "alias");
        order.verify(documents).selectEnabledByObjectCode(7L, "alias");
        order.verify(objects).selectByConfigKey(7L, "cfg");
        order.verify(objects).selectByConfigKey(7L, "alias");
        order.verify(objects).selectFirstByObjectCode(7L, "alias");
        order.verify(configs).selectPublishedByObjectCodeOrConfigKey(7L, "cfg");
        order.verify(objects).selectByConfigKey(7L, "cfg");
        order.verify(objects).selectByConfigKey(7L, "generic");
        order.verify(objects).selectFirstByObjectCode(7L, "generic");
        order.verify(links).selectLatestByBusinessKey(7L, "generic:21");
        TenantContextHolder.setTenantId(19L);
        service.getFlowStatus("generic", 22L);
        verify(links).selectLatestByBusinessKey(19L, "generic:22");
        verifyNoInteractions(records);
    }

    @Test
    void statusProjectsExistingLinkAndRejectsMissingIdentityBeforeQuery() {
        var link = new AiBusinessFlowInstanceLink();
        link.setId(8L);
        link.setObjectCode("generic");
        link.setRecordId(21L);
        link.setBusinessKey("generic:21");
        link.setFlowStatus("RUNNING");
        link.setProcessInstanceId("instance-1");
        when(links.selectLatestByBusinessKey(7L, "generic:21")).thenReturn(link);
        var status = service.getFlowStatus("generic", 21L);
        assertEquals(8L, status.getLinkId());
        assertEquals("RUNNING", status.getFlowStatus());
        assertEquals("instance-1", status.getProcessInstanceId());
        clearInvocations(links, configs, documents, objects);
        assertThrows(BusinessException.class, () -> service.getFlowStatus(" ", 21L));
        assertThrows(BusinessException.class, () -> service.getFlowStatus("generic", null));
        verifyNoInteractions(links, configs, documents, objects);
    }

    @Test
    void engineStatusPreservesSuccessAndRecoverableFailureContract() {
        assertNull(service.getFlowStatus("generic:21"));
        FlowClient client = mock(FlowClient.class);
        ReflectionTestUtils.setField(service, "flowClient", client);
        when(client.getProcessStatus("generic:21")).thenReturn(FlowResult.success(Map.of("state", "running")));
        assertEquals(Map.of("state", "running"), service.getFlowStatus("generic:21").get("data"));
        when(client.getProcessStatus("generic:21")).thenReturn(FlowResult.error("unavailable"));
        assertNull(service.getFlowStatus("generic:21"));
        when(client.getProcessStatus("generic:21")).thenThrow(new IllegalStateException("unavailable"));
        assertNull(service.getFlowStatus("generic:21"));
    }

    @Test
    void lowcodeListBatchesDeduplicatesAndPreservesDisplayPrecedence() {
        aliasDocument();
        BusinessObjectVO object = new BusinessObjectVO();
        object.setObjectName("Generic object");
        object.setOptions("{\"summaryExpression\":\"Record ${orderNo}\"}");
        when(objects.selectObjectList(eq(7L), any())).thenReturn(List.of(object));
        when(records.selectByIds("cfg", List.of(21L, 22L)))
                .thenReturn(Map.of("21", Map.of("order_no", "A21"), 22L, Map.of("order_no", "A22")));
        var first = item("alias:21");
        first.setProcessDefinitionName("Existing title");
        var duplicate = item("alias:21");
        var second = item("alias:22");
        service.enrichBusinessListDisplay(Arrays.asList(first, null, duplicate, second));
        assertEquals("generic", first.getObjectCode());
        assertEquals("Generic object", first.getBusinessObjectName());
        assertEquals("Record A21", first.getBusinessSummary());
        assertEquals("Record A22", second.getBusinessSummary());
        assertEquals("Existing title", first.getProcessDefinitionName());
        assertEquals("Process name", second.getProcessDefinitionName());
        verify(links).selectLatestByBusinessKeys(7L, Set.of("alias:21", "alias:22"));
        verify(configs).selectPublishedByObjectCodeOrConfigKey(7L, "alias");
        verify(records, times(1)).selectByIds("cfg", List.of(21L, 22L));
        verifyNoInteractions(providers);
    }

    @Test
    void codeListUsesLinkedIdentityAndProviderBatchWithoutLowcodeAccess() {
        var link = new AiBusinessFlowInstanceLink();
        link.setBusinessKey("alias:21");
        link.setObjectCode("generic");
        link.setRecordId(77L);
        when(links.selectLatestByBusinessKeys(7L, Set.of("alias:21"))).thenReturn(List.of(link));
        when(providers.listAssets("generic", true)).thenReturn(List.of(Map.of("providerKey", "provider", "appName", "Generic app")));
        var provider = mock(BusinessCodeFormProvider.class);
        when(providers.find("provider")).thenReturn(Optional.of(provider));
        when(provider.buildSummaries("generic", List.of(77L))).thenReturn(Map.of(77L, "Provider summary"));
        var item = item("alias:21");
        service.enrichBusinessListDisplay(List.of(item));
        assertEquals(77L, item.getRecordId());
        assertEquals("generic", item.getObjectCode());
        assertEquals("Generic app", item.getBusinessObjectName());
        assertEquals("Provider summary", item.getBusinessSummary());
        verify(provider).buildSummaries("generic", List.of(77L));
        verifyNoInteractions(records);
    }

    @Test
    void missingAssetsAndMalformedKeysKeepExistingSummaryWithoutExtraReads() {
        service.enrichBusinessListDisplay(null);
        service.enrichBusinessListDisplay(List.of());
        verifyNoInteractions(links, configs, records);
        var item = item("generic:21");
        item.setBusinessSummary("Keep summary");
        var malformed = item("generic:invalid");
        service.enrichBusinessListDisplay(List.of(item, malformed));
        assertEquals("Keep summary", item.getBusinessSummary());
        assertEquals("generic", item.getBusinessObjectName());
        assertEquals("Process name", malformed.getProcessDefinitionName());
        verify(configs, times(2)).selectPublishedByObjectCodeOrConfigKey(7L, "generic");
        verifyNoInteractions(records);
    }

    @Test
    void permissionFailureFromAuthorizedRecordReaderPropagatesUnchanged() {
        aliasDocument();
        BusinessException denied = new BusinessException("Denied");
        when(records.selectByIds("cfg", List.of(21L))).thenThrow(denied);
        assertSame(denied, assertThrows(BusinessException.class,
                () -> service.enrichBusinessListDisplay(List.of(item("alias:21")))));
        verifyNoInteractions(providers);
    }

    private void aliasDocument() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setObjectCode("generic");
        document.setConfigKey("cfg");
        when(documents.selectEnabledByObjectCode(7L, "alias")).thenReturn(document);
    }

    private FlowBusinessListDisplayItem item(String businessKey) {
        var item = new FlowBusinessListDisplayItem();
        item.setBusinessKey(businessKey);
        item.setProcessName("Process name");
        item.setProcessDefKey("process-key");
        return item;
    }
}
