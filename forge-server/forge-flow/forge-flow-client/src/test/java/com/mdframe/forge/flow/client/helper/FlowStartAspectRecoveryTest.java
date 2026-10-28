package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.FlowStartOutcomeUnknownException;
import com.mdframe.forge.flow.client.annotation.FlowStart;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowStartAspectRecoveryTest {

    @Test
    void remoteStartSeesCommittedBusinessRowAndRollbackNeverSends() throws Throwable {
        Fixture fixture = new Fixture();
        when(fixture.send()).thenAnswer(invocation -> {
            try (Connection connection = fixture.dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM business_row")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            return FlowResult.success("process-1");
        });
        fixture.transaction.executeWithoutResult(status -> {
            fixture.jdbc.update("INSERT INTO business_row VALUES (1)");
            assertEquals("case:1", assertDoesNotThrow(fixture::invoke));
            verifyNoInteractions(fixture.client);
        });
        verify(fixture.client).startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), eq("round-1"), isNull());
        clearInvocations(fixture.client);
        fixture.transaction.executeWithoutResult(status -> {
            assertDoesNotThrow(fixture::invoke);
            status.setRollbackOnly();
        });
        verifyNoInteractions(fixture.client);
    }

    @Test
    void uncertainStartQueriesExactRoundOnceAndAcceptsOnlyItsStartedResult() throws Throwable {
        Fixture fixture = new Fixture();
        when(fixture.send()).thenThrow(fixture.unknown);
        when(fixture.client.getProcessStartStatus("case:1", "round-1"))
                .thenReturn(FlowResult.success(new FlowStartOutcome(FlowStartOutcome.State.STARTED,
                        "case:1", "round-1", "process-1", "RUNNING", 1)));

        assertEquals("case:1", fixture.invoke());
        verify(fixture.client).getProcessStartStatus("case:1", "round-1");
        assertEquals(2, mockingDetails(fixture.client).getInvocations().size());
    }

    @ParameterizedTest
    @EnumSource(value = FlowStartOutcome.State.class, names = {"NOT_FOUND", "IN_PROGRESS"})
    void unresolvedOutcomeEscapesAfterCommitAndDoesNotResend(FlowStartOutcome.State state) throws Throwable {
        Fixture fixture = new Fixture();
        when(fixture.send()).thenThrow(fixture.unknown);
        when(fixture.client.getProcessStartStatus("case:1", "round-1"))
                .thenReturn(FlowResult.success(new FlowStartOutcome(state, "case:1", "round-1", null, null, null)));

        assertSame(fixture.unknown, assertThrows(FlowStartOutcomeUnknownException.class,
                () -> fixture.transaction.executeWithoutResult(status -> {
                    fixture.jdbc.update("INSERT INTO business_row VALUES (1)");
                    assertDoesNotThrow(fixture::invoke);
                })));
        assertEquals(1, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM business_row", Integer.class));
        assertEquals("round-1", fixture.unknown.getStartRequestId());
        assertEquals(2, mockingDetails(fixture.client).getInvocations().size());
    }

    @Test
    void wrongRoundAndQueryFailureRemainUnknown() throws Throwable {
        Fixture fixture = new Fixture();
        when(fixture.send()).thenThrow(fixture.unknown);
        when(fixture.client.getProcessStartStatus("case:1", "round-1"))
                .thenReturn(FlowResult.success(new FlowStartOutcome(FlowStartOutcome.State.STARTED,
                        "case:1", "other-round", "other-process", "RUNNING", 2)));
        assertSame(fixture.unknown, assertThrows(FlowStartOutcomeUnknownException.class, fixture::invoke));
        IllegalStateException failure = new IllegalStateException("query unavailable");
        when(fixture.client.getProcessStartStatus("case:1", "round-1")).thenThrow(failure);
        assertSame(fixture.unknown, assertThrows(FlowStartOutcomeUnknownException.class, fixture::invoke));
        assertArrayEquals(new Throwable[]{failure}, fixture.unknown.getSuppressed());
    }

    @Test
    void definiteRejectionIsVisibleAndNotQueried() throws Throwable {
        Fixture fixture = new Fixture();
        when(fixture.send()).thenReturn(FlowResult.error("not allowed"));
        assertEquals("not allowed", assertThrows(FlowClientException.class, fixture::invoke).getMessage());
        verify(fixture.client, never()).getProcessStartStatus(anyString(), anyString());
    }

    @Test
    void omittedRequestIdentityIsStableAcrossRepeatedInvocations() throws Throwable {
        Fixture fixture = new Fixture("defaultIdentity");
        when(fixture.client.startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), anyString(), isNull()))
                .thenReturn(FlowResult.success("process-1"));
        fixture.invoke();
        fixture.invoke();
        ArgumentCaptor<String> identities = ArgumentCaptor.forClass(String.class);
        verify(fixture.client, times(2)).startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), identities.capture(), isNull());
        assertFalse(identities.getAllValues().get(0).isBlank());
        assertEquals(identities.getAllValues().get(0), identities.getAllValues().get(1));
    }

    @Test
    void explicitResubmissionRetainsItsRequestAndPredecessor() throws Throwable {
        Fixture fixture = new Fixture("resubmission");
        when(fixture.client.startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), eq("round-2"), eq("process-1")))
                .thenReturn(FlowResult.success("process-2"));
        assertEquals("case:1", fixture.invoke());
        verify(fixture.client).startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), eq("round-2"), eq("process-1"));
    }

    @Test
    void failedIdentityExpressionsNeverFallBackToAFirstRound() throws Throwable {
        for (String method : new String[]{"missingRequest", "missingPredecessor"}) {
            Fixture fixture = new Fixture(method);
            assertThrows(FlowClientException.class, fixture::invoke);
            verifyNoInteractions(fixture.client);
        }
    }

    @Test
    void aStartFailureDoesNotReplaceTheOriginalBusinessFailure() throws Throwable {
        Fixture fixture = new Fixture("startOnError");
        IllegalStateException businessFailure = new IllegalStateException("business failed");
        FlowClientException startFailure = new FlowClientException("start rejected");
        when(fixture.joinPoint.proceed()).thenThrow(businessFailure);
        when(fixture.send()).thenThrow(startFailure);
        assertSame(businessFailure, assertThrows(IllegalStateException.class, fixture::invoke));
        assertArrayEquals(new Throwable[]{startFailure}, businessFailure.getSuppressed());
    }

    private static final class Fixture {
        private final FlowClient client = mock(FlowClient.class);
        private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        private final FlowStartAspect aspect = new FlowStartAspect(client, mock(ApplicationContext.class));
        private final FlowStartOutcomeUnknownException unknown =
                new FlowStartOutcomeUnknownException("case:1", "round-1", new IllegalStateException("timeout"));
        private final JdbcDataSource dataSource = new JdbcDataSource();
        private final JdbcTemplate jdbc;
        private final TransactionTemplate transaction;
        private final FlowStart annotation;

        private Fixture() throws Throwable {
            this("business");
        }

        private Fixture(String methodName) throws Throwable {
            dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE business_row(id INT PRIMARY KEY)");
            transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            Method method = Fixture.class.getDeclaredMethod(methodName);
            annotation = method.getAnnotation(FlowStart.class);
            MethodSignature signature = mock(MethodSignature.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
            when(joinPoint.getTarget()).thenReturn(this);
            when(joinPoint.proceed()).thenReturn("case:1");
        }

        private FlowResult<String> send() {
            return client.startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                    isNull(), isNull(), isNull(), isNull(), eq("round-1"), isNull());
        }

        private Object invoke() throws Throwable {
            return aspect.around(joinPoint, annotation);
        }

        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "'round-1'")
        private String business() {
            return "case:1";
        }

        @FlowStart(modelKey = "model", businessKeySpEl = "#result")
        private String defaultIdentity() { return "case:1"; }

        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "'round-2'",
                previousProcessInstanceIdSpEl = "'process-1'")
        private String resubmission() { return "case:1"; }

        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "#absent")
        private String missingRequest() { return "case:1"; }

        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "'round-2'",
                previousProcessInstanceIdSpEl = "#absent")
        private String missingPredecessor() { return "case:1"; }

        @FlowStart(modelKey = "model", businessKeySpEl = "'case:1'", startRequestIdSpEl = "'round-1'",
                skipOnError = false)
        private String startOnError() { return "case:1"; }
    }
}
