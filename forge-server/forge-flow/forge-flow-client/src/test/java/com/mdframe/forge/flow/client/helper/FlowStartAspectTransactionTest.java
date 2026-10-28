package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowStart;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowStartAspectTransactionTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void transactionProxyCommitsBeforeStartAndNeverSendsForRollback(boolean transactionOuter) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE business_row(id INT PRIMARY KEY)");
        FlowClient client = mock(FlowClient.class);
        when(client.startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), eq("round-1"), isNull()))
                .thenAnswer(invocation -> {
                    try (Connection connection = dataSource.getConnection();
                         Statement statement = connection.createStatement();
                         ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM business_row")) {
                        assertTrue(rows.next());
                        assertEquals(1, rows.getInt(1));
                    }
                    return FlowResult.success("process-1");
                });

        AspectJProxyFactory factory = new AspectJProxyFactory(new BusinessService(jdbc));
        FlowStartAspect aspect = new FlowStartAspect(client, mock(ApplicationContext.class));
        TransactionInterceptor transaction = new TransactionInterceptor(
                new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource());
        if (transactionOuter) {
            factory.addAdvice(transaction);
            factory.addAspect(aspect);
        } else {
            factory.addAspect(aspect);
            factory.addAdvice(transaction);
        }
        BusinessService proxy = factory.getProxy();
        assertEquals("case:1", proxy.submit());
        verify(client).startProcess(eq("model"), eq("case:1"), eq(""), isNull(), anyMap(),
                isNull(), isNull(), isNull(), isNull(), eq("round-1"), isNull());
        clearInvocations(client);
        assertThrows(IllegalStateException.class, proxy::rollback);
        verifyNoInteractions(client);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM business_row", Integer.class));
    }

    public static class BusinessService {
        private final JdbcTemplate jdbc;

        BusinessService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Transactional
        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "'round-1'")
        public String submit() {
            jdbc.update("INSERT INTO business_row VALUES (1)");
            return "case:1";
        }

        @Transactional
        @FlowStart(modelKey = "model", businessKeySpEl = "#result", startRequestIdSpEl = "'round-2'")
        public String rollback() {
            jdbc.update("INSERT INTO business_row VALUES (2)");
            throw new IllegalStateException("business rollback");
        }
    }
}
