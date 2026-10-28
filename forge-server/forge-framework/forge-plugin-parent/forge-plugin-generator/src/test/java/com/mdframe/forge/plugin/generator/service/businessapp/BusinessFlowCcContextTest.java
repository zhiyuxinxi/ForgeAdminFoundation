package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BusinessFlowCcContextTest {
    @Test
    void verifiedIdentityUsesResponseAndFallsBackToNestedVariables() {
        FlowClient client = mock(FlowClient.class);
        Map<String, Object> data = Map.of("taskId", " task-1 ", "businessKey", "example:42",
                "recordId", "invalid", "variables", Map.of("objectCode", "example", "recordId", 42L,
                        "formKey", "detail"));
        FlowResult<Map<String, Object>> result = new FlowResult<>();
        result.setCode(200);
        result.setData(data);
        when(client.getCcFormInfo("cc-1")).thenReturn(result);
        CcTaskFormLookup.VerifiedForm form = CcTaskFormLookup.read(client, " cc-1 ");
        assertThat(form.query().getTaskId()).isEqualTo("task-1");
        assertThat(form.query().getRecordId()).isEqualTo(42L);
        assertThat(form.query().getObjectCode()).isEqualTo("example");
        assertThat(form.query().getFormKey()).isEqualTo("detail");
        assertThat(form.data()).isSameAs(data);
    }

    @Test
    void missingCcIdentityFailsBeforeAnyFlowLookup() {
        BusinessFlowService service = mock(BusinessFlowService.class, CALLS_REAL_METHODS);
        FlowClient client = mock(FlowClient.class);
        ReflectionTestUtils.setField(service, "flowClient", client);
        assertThatThrownBy(() -> service.getCcTaskFormReadonlyContext(" "))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(client);
    }

    @Test
    void unconfiguredFlowClientDoesNotBuildABusinessContext() {
        BusinessFlowService service = mock(BusinessFlowService.class, CALLS_REAL_METHODS);
        assertThatThrownBy(() -> service.getCcTaskFormReadonlyContext("cc-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectedRecipientCheckDoesNotBuildABusinessContext() {
        BusinessFlowService service = mock(BusinessFlowService.class, CALLS_REAL_METHODS);
        FlowClient client = mock(FlowClient.class);
        ReflectionTestUtils.setField(service, "flowClient", client);
        FlowResult<Map<String, Object>> denied = new FlowResult<>();
        denied.setCode(403);
        denied.setMsg("Access denied");
        when(client.getCcFormInfo("cc-1")).thenReturn(denied);
        assertThatThrownBy(() -> service.getCcTaskFormReadonlyContext(" cc-1 "))
                .isInstanceOf(BusinessException.class).hasMessage("Access denied");
        verify(client).getCcFormInfo("cc-1");
    }
}
