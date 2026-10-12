package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.impl.GenDatasourceServiceImpl;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.plugin.generator.support.GeneratorJdbcDataSources;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Persistence and pool lifecycle stay at the design boundary. No JDBC connections are opened. */
class GeneratorRuntimeDataSourceAccessTest {
    private final GenDatasourceMapper mapper = mock(GenDatasourceMapper.class);
    private final GeneratorJdbcDataSources pools = mock(GeneratorJdbcDataSources.class);
    private final GeneratorRuntimeDataSourceAccess access = new GeneratorRuntimeDataSourceAccess(mapper, pools);

    private GenDatasource datasource(String code) {
        GenDatasource value = new GenDatasource();
        value.setDatasourceId(7L);
        value.setDatasourceCode(code);
        value.setDatasourceName("Runtime source");
        value.setIsEnabled(1);
        value.setReadonly(0);
        value.setAllowRuntimeWrite(1);
        value.setAllowRuntimeDdl(0);
        return value;
    }

    @Test
    void expiredIdAndCodeEntriesRefreshWithoutWaitingOrCachingMisses() throws ReflectiveOperationException {
        GenDatasource original = datasource("before");
        GenDatasource changed = datasource("after");
        when(mapper.selectById(7L)).thenReturn(original, changed);
        access.getDatasourceById(7L);
        expire("datasourceByIdCache", 7L);
        assertEquals("after", access.getDatasourceById(7L).getDatasourceCode());
        verify(mapper, times(2)).selectById(7L);

        access.clearDatasourceCache();
        when(mapper.selectOne(any())).thenReturn(original, changed);
        access.resolveSnapshot(null, "before", "table", "EXISTING");
        expire("datasourceByCodeCache", "before");
        assertEquals("after", access.resolveSnapshot(null, "before", "table", "EXISTING").getDatasourceCode());
        verify(mapper, times(2)).selectOne(any());
        assertNull(access.getDatasourceById(99L));
        assertNull(access.getDatasourceById(99L));
        verify(mapper, times(2)).selectById(99L);
    }

    private void expire(String field, Object key) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        Map<Object, Object> cache = (Map<Object, Object>) ReflectionTestUtils.getField(access, field);
        Object old = cache.get(key);
        var value = old.getClass().getDeclaredMethod("value");
        value.setAccessible(true);
        var constructor = old.getClass().getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        cache.put(key, constructor.newInstance(value.invoke(old), 0L));
    }

    @Test
    void actualDesignUpdateInvalidatesBothCodesAndClosesExistingPool() {
        GenDatasource existing = datasource("old_code");
        GenDatasource updated = datasource("new_code");
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.updateById(any(GenDatasource.class))).thenReturn(1);
        access.resolveSnapshot(null, "old_code", "records", "EXISTING");
        LowcodeRuntimeDataSourceResolver resolver = new LowcodeRuntimeDataSourceResolver(access, new ObjectMapper());
        GenDatasourceServiceImpl service = new GenDatasourceServiceImpl(mapper, null, resolver, pools);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        assertTrue(service.updateById(updated));
        verify(pools).removeDataSource(7L);
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(access, "datasourceByIdCache")).isEmpty());
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(access, "datasourceByCodeCache")).isEmpty());
    }

    @Test
    void actualDesignDeleteInvalidatesCachedDefinitionAndClosesPool() {
        GenDatasource existing = datasource("old_code");
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.deleteById((java.io.Serializable) 7L)).thenReturn(1);
        access.getDatasourceById(7L);
        LowcodeRuntimeDataSourceResolver resolver = new LowcodeRuntimeDataSourceResolver(access, new ObjectMapper());
        GenDatasourceServiceImpl service = new GenDatasourceServiceImpl(mapper, null, resolver, pools);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        assertTrue(service.removeById(7L));
        verify(pools).removeDataSource(7L);
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(access, "datasourceByIdCache")).isEmpty());
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(access, "datasourceByCodeCache")).isEmpty());
    }

    @Test
    void springWiresLegacyResolverAndJdbcConsumerToTheSameDesignAdapter() {
        DataSource master = mock(DataSource.class);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(GenDatasourceMapper.class, () -> mapper);
            context.registerBean(GeneratorJdbcDataSources.class, () -> pools);
            context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(master));
            context.registerBean(NamedParameterJdbcTemplate.class, () -> new NamedParameterJdbcTemplate(master));
            context.register(GeneratorRuntimeDataSourceAccess.class, LowcodeRuntimeDataSourceResolver.class,
                    RuntimeJdbcTemplateProvider.class);
            context.refresh();
            RuntimeDataSourceAccess actual = context.getBean(RuntimeDataSourceAccess.class);
            when(mapper.selectById(7L)).thenReturn(datasource("live"));
            assertEquals("live", context.getBean(LowcodeRuntimeDataSourceResolver.class)
                    .getDatasourceById(7L).getDatasourceCode());
            actual.resolveSnapshot(7L, null, "records", "EXISTING");
            verify(mapper).selectById(7L);
            assertSame(master, context.getBean(RuntimeJdbcTemplateProvider.class).resolveDataSource(null));
            verifyNoInteractions(pools, master);
        }
    }

    @Test
    void runtimeContractAndPureOwnerLoadFromIsolatedArtifacts() {
        assertTrue(RuntimeDataSourceAccess.class.getProtectionDomain().getCodeSource().getLocation()
                .toString().endsWith("forge-lowcode-api-runtime-datasource.jar"));
        assertTrue(RuntimeDataSourceResolution.class.getProtectionDomain().getCodeSource().getLocation()
                .toString().endsWith("runtime-classes/"));
    }
}
