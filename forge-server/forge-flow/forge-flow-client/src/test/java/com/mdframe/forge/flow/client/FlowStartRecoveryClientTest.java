package com.mdframe.forge.flow.client;

import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowStartRecoveryClientTest {
    @Test
    void uncertainResponsesKeepOriginalIdentityAndNeverRetryAutomatically() {
        Object[] responses = {new ResourceAccessException("read timed out", new SocketTimeoutException()),
                new HttpClientErrorException(HttpStatus.REQUEST_TIMEOUT),
                new HttpServerErrorException(HttpStatus.BAD_GATEWAY), ResponseEntity.ok(""),
                ResponseEntity.ok("not json"), ResponseEntity.ok("{\"code\":200,\"data\":null}")};
        for (Object response : responses) {
            var rest = mock(RestTemplate.class);
            when(rest.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenAnswer(call -> {
                        if (response instanceof RuntimeException exception) {
                            throw exception;
                        }
                        return response;
                    });
            var client = new FlowClient(rest, "http://flow", "current-token");
            var error = assertThrows(FlowStartOutcomeUnknownException.class, () -> start(client));
            assertEquals("business-1", error.getBusinessKey());
            assertEquals("request-1", error.getStartRequestId());
            verify(rest, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        }
    }

    @Test
    void preparationAndExplicitRejectionRemainKnownFailures() {
        var rest = mock(RestTemplate.class);
        var client = new FlowClient(rest, "http://flow", "static-token");
        client.setTokenProvider(() -> {
            throw new FlowTokenAcquisitionException("unavailable", null);
        });
        FlowClientException error = assertThrows(FlowClientException.class, () -> start(client));
        assertFalse(error instanceof FlowStartOutcomeUnknownException);
        verifyNoInteractions(rest);

        client.setTokenProvider(null);
        when(rest.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                .thenReturn(ResponseEntity.ok("{\"code\":409,\"msg\":\"request conflict\"}"));
        error = assertThrows(FlowClientException.class, () -> start(client));
        assertFalse(error instanceof FlowStartOutcomeUnknownException);
        assertFalse(start(client).isSuccess());
    }

    @Test
    void successfulSynchronousEntryPointsKeepRealInstanceResults() {
        var rest = mock(RestTemplate.class);
        when(rest.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":\"instance-1\"}"));
        var client = new FlowClient(rest, "http://flow", "current-token");
        assertEquals("instance-1", start(client).getData());
        assertEquals("instance-1", client.startProcessForDelegatedUser(
                "approval", "business-1", "type", "title", Map.of(), "request-1", null).getData());
        assertEquals("instance-1", client.startHighRiskApprovalForDelegatedUser(
                "approval", "business-1", "title", Map.of()).getData());
    }

    @Test
    void exactQueryEncodesIdentityAndDoesNotFallBackToLatestBusinessStatus() {
        var rest = mock(RestTemplate.class);
        URI url = URI.create("http://flow/api/flow/instance/status/order%3A1%20%26%202?startRequestId=round%3A1");
        when(rest.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"code":200,"data":{"state":"STARTED","businessKey":"order:1 & 2",
                        "startRequestId":"round:1","processInstanceId":"original","processStatus":"rejected","roundNo":1}}
                        """));
        var client = new FlowClient(rest, "http://flow", "current-token");
        var result = client.getProcessStartStatus("order:1 & 2", "round:1");
        assertTrue(result.isSuccess());
        assertEquals(FlowStartOutcome.State.STARTED, result.getData().state());
        assertEquals("original", result.getData().processInstanceId());
        assertThrows(FlowClientException.class, () -> client.getProcessStartStatus("business", ""));
        verify(rest, times(1)).exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    private FlowResult<String> start(FlowClient client) {
        return client.startProcess("approval", "business-1", "type", "title", Map.of(),
                "501", "Owner", "1", "Department", "request-1", null);
    }
}
