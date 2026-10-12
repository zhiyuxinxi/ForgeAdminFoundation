package com.mdframe.forge.flow.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.starter.auth.config.FlowDelegationSessionVerifier;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.flow.dto.FlowStartRoundRequest;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowMonitorService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowStartRoundContractTest {
    @Test
    void clientRoundFieldsReachServerOutsideVariablesWithTrustedIdentity() {
        var service = mock(FlowInstanceService.class);
        var verifier = mock(FlowDelegationSessionVerifier.class);
        var controller = controller(service, verifier);
        var transport = mock(RestTemplate.class);
        when(transport.exchange(eq("http://flow/api/flow/instance/start-delegated/approval"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenAnswer(call -> {
                    HttpEntity<String> entity = call.getArgument(2);
                    Map<String, Object> body = new ObjectMapper().readValue(entity.getBody(), new TypeReference<>() {});
                    assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer delegated");
                    try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
                        controller.startDelegated("approval", body);
                    }
                    return ResponseEntity.ok("{\"code\":200,\"data\":\"next-instance\"}");
                });
        var client = new FlowClient(transport, "http://flow", "static");
        client.setTokenProvider(() -> "delegated");

        assertThat(client.startProcessForDelegatedUser("approval", "record:1", "record", "Title",
                Map.of("reviewer", "reviewer"), "attempt-2", "previous-instance").isSuccess()).isTrue();
        verify(service).startProcess("approval", "record:1", "record", "Title",
                Map.of("reviewer", "reviewer"), "101", "Applicant", "201", "Department",
                new FlowStartRoundRequest("attempt-2", "previous-instance"));
        verify(verifier).requireTrustedDelegation();
    }

    @Test
    void legacyRequestUsesLegacyOverloadAndMalformedRoundCannotBecomeVariables() {
        var service = mock(FlowInstanceService.class);
        var controller = controller(service, mock(FlowDelegationSessionVerifier.class));
        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            controller.startDelegated("approval", Map.of("businessKey", "record:1", "title", "Title"));
        }
        verify(service).startProcess("approval", "record:1", "approval", "Title", Map.of(),
                "101", "Applicant", "201", "Department");
        var untouched = mock(FlowInstanceService.class);
        var invalid = controller(untouched, mock(FlowDelegationSessionVerifier.class));
        assertThatThrownBy(() -> invalid.start("approval", Map.of(
                "businessKey", "record:1", "previousProcessInstanceId", "old")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> invalid.start("approval", Map.of("startRequestId", "attempt-1")))
                .hasMessage("FLOW_START_BUSINESS_KEY_REQUIRED");
        assertThatThrownBy(() -> new FlowStartRoundRequest("中文", null))
                .hasMessage("FLOW_START_REQUEST_ID_INVALID");
        verifyNoInteractions(untouched);
    }

    private FlowInstanceController controller(FlowInstanceService service, FlowDelegationSessionVerifier verifier) {
        return new FlowInstanceController(service, mock(FlowMonitorService.class),
                mock(FlowOrgIntegrationService.class), verifier);
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setRealName("Applicant");
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setActiveOrgName("Department");
        return new ExecutionIdentity(user, "USER", 101L, 999L, 301L,
                "agent-client", "token", Set.of("ai:businessFlow:start"));
    }
}
