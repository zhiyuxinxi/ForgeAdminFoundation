package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DynamicCrudTombstoneTest {
    @Test
    void numericAndStringKeysPermitRepeatedBusinessKeyReuseWithoutOverwritingHistory() {
        verifyTombstones("BIGINT", "BIGINT", 11L, 12L, 13L);
        verifyTombstones("VARCHAR(32)", "VARCHAR(32)", "record-a", "record-b", "record-c");
    }

    private void verifyTombstones(String idType, String flagType, Object first, Object second, Object third) {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE records(record_key " + idType + " PRIMARY KEY, code VARCHAR(30), del_flag "
                + flagType + " DEFAULT 0, UNIQUE(code,del_flag))");
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(source);
        RuntimeJdbcTemplateProvider provider = mock(RuntimeJdbcTemplateProvider.class);
        when(provider.namedJdbcTemplate(any())).thenReturn(named);
        DynamicCrudRepository repository = spy(new DynamicCrudRepository(named, provider, mock(RuntimeDatabaseDialectFactory.class)));
        doReturn(true).when(repository).tableExists("records");
        doReturn(Set.of("record_key", "code", "del_flag")).when(repository).getTableColumns("records");
        LowcodeRuntimeDataSourceContext context = context();
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = LowcodeRuntimeDataSourceContextHolder.use(context)) {
            jdbc.update("INSERT INTO records(record_key,code) VALUES(?, 'same')", first);
            assertEquals(1, repository.deleteById("records", "record_key", first, true, null));
            jdbc.update("INSERT INTO records(record_key,code) VALUES(?, 'same')", second);
            assertEquals(1, repository.deleteById("records", "record_key", second, true, null));
            jdbc.update("INSERT INTO records(record_key,code) VALUES(?, 'same')", third);
            assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM records", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM records WHERE del_flag='0'", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM records WHERE del_flag=record_key", Integer.class));
        }
    }

    private LowcodeRuntimeDataSourceContext context() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master("records");
        LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
        primaryKey.setColumnName("record_key");
        context.setPrimaryKey(primaryKey);
        LowcodeLogicDeleteStrategy delete = new LowcodeLogicDeleteStrategy();
        delete.setMode("DEL_FLAG");
        delete.setColumnName("del_flag");
        delete.setDeletedValueMode("PRIMARY_KEY");
        context.setLogicDeleteStrategy(delete);
        LowcodeTenantStrategy tenant = new LowcodeTenantStrategy();
        tenant.setMode("NONE");
        context.setTenantStrategy(tenant);
        LowcodeAuditStrategy audit = new LowcodeAuditStrategy();
        audit.setMode("NONE");
        context.setAuditStrategy(audit);
        return context;
    }
}
