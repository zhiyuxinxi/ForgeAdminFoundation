package com.mdframe.forge.business.core.purchase.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.business.core.purchase.domain.SamplePurchaseOrder;
import com.mdframe.forge.business.core.purchase.service.impl.SamplePurchaseOrderServiceImpl;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.lowcode.api.binding.BusinessFlowBinding;
import com.mdframe.forge.lowcode.api.binding.BusinessFlowBindingQuery;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SamplePurchaseOrderFlowBindingTest {

    private static final long TENANT_ID = 7L;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void rendersTitleFromResolvedPublicBindingProjection() {
        BusinessFlowBindingQuery bindingQuery = mock(BusinessFlowBindingQuery.class);
        when(bindingQuery.findForObject(TENANT_ID, SamplePurchaseOrderService.BUSINESS_TYPE))
                .thenReturn(Optional.of(new BusinessFlowBinding(
                        31L, "{\"titleTemplate\":\"采购-${orderNo}\"}")));

        String title = configuredTitle(bindingQuery);

        assertThat(title).isEqualTo("采购-PO-001");
        verify(bindingQuery).findForObject(TENANT_ID, SamplePurchaseOrderService.BUSINESS_TYPE);
    }

    @Test
    void fallsBackToOriginalTitleWhenNoFlowOrApprovalBindingExists() {
        BusinessFlowBindingQuery bindingQuery = mock(BusinessFlowBindingQuery.class);
        when(bindingQuery.findForObject(TENANT_ID, SamplePurchaseOrderService.BUSINESS_TYPE))
                .thenReturn(Optional.empty());

        assertThat(configuredTitle(bindingQuery)).isEqualTo("采购单审批-PO-001");
    }

    @Test
    void fallsBackToOriginalTitleWhenResolvedBindingConfigIsInvalid() {
        BusinessFlowBindingQuery bindingQuery = mock(BusinessFlowBindingQuery.class);
        when(bindingQuery.findForObject(TENANT_ID, SamplePurchaseOrderService.BUSINESS_TYPE))
                .thenReturn(Optional.of(new BusinessFlowBinding(32L, "{invalid-json")));

        assertThat(configuredTitle(bindingQuery)).isEqualTo("采购单审批-PO-001");
    }

    private String configuredTitle(BusinessFlowBindingQuery bindingQuery) {
        TenantContextHolder.setTenantId(TENANT_ID);
        SamplePurchaseOrderServiceImpl service = new SamplePurchaseOrderServiceImpl(
                mock(FlowClient.class), mock(IdentifierGenerator.class), bindingQuery);
        SamplePurchaseOrder order = new SamplePurchaseOrder();
        order.setId(1001L);
        order.setOrderNo("PO-001");
        return ReflectionTestUtils.invokeMethod(
                service,
                "buildConfiguredFlowTitle",
                order,
                SamplePurchaseOrderService.BUSINESS_TYPE + ":1001",
                Map.of());
    }
}
