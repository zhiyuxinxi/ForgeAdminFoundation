package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real mapper XML and production tenant/data-scope interceptors on an isolated H2 database. */
final class RoleResourceQueryFixture implements AutoCloseable {
    final IDataScopeService scopes = mock(IDataScopeService.class);
    final TenantProperties tenant = new TenantProperties();
    final SqlSession session;
    final JdbcTemplate jdbc;
    final SysResourceMapper resources;
    final SysRoleResourceMapper bindings;

    RoleResourceQueryFixture() throws IOException {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:role_resource_" + UUID.randomUUID() + ";MODE=MySQL");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
        // Exercise tenant-scoped deployments; the default global resource catalog is tested separately.
        tenant.setIgnoreTables(List.of());
        tenant.setStrictMode(true);
        tenant.setAutoDetectTenantColumn(false);
        MybatisPlusInterceptor interceptors = new MybatisPlusInterceptor();
        interceptors.addInnerInterceptor(new TenantLineInnerInterceptor(new DefaultTenantLineHandler(tenant)));
        DataScopeProperties scope = new DataScopeProperties();
        scope.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        interceptors.addInnerInterceptor(new DataScopeInterceptor(scopes, scope));
        configuration.addInterceptor(interceptors);
        when(scopes.getDataScopeConfig(anyString())).thenAnswer(call -> {
            String id = call.getArgument(0);
            return id.endsWith(".selectList") || id.endsWith(".selectBatchIds") ? policy(false) : null;
        });
        for (Class<?> mapper : new Class<?>[]{SysResourceMapper.class, SysRoleResourceMapper.class}) {
            Path xml = Path.of(System.getProperty("role.resource.xml", "src/main/resources/mapper"),
                    mapper.getSimpleName() + ".xml");
            try (var input = Files.newInputStream(xml)) {
                new XMLMapperBuilder(input, configuration, xml.toString(), configuration.getSqlFragments()).parse();
            }
        }
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(session.getConnection(), true));
        resources = session.getMapper(SysResourceMapper.class);
        bindings = session.getMapper(SysRoleResourceMapper.class);
        createTable(SysResource.class);
        createTable(SysRoleResource.class);
        TenantContextHolder.setTenantId(20L);
        seed();
    }

    void restrictToCreator(String method, long creator) {
        when(scopes.getDataScopeConfig(SysResourceMapper.class.getName() + "." + method)).thenReturn(policy(true));
        DataScopeContext context = new DataScopeContext();
        context.setUserId(creator);
        context.setTenantId(20L);
        context.setMinDataScope(5);
        when(scopes.getCurrentUserDataScope("role-resource-query")).thenReturn(context);
    }

    SysDataScopeConfig policy(boolean enabled) {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setEnabled(enabled ? 1 : 0);
        config.setResourceCode("role-resource-query");
        config.setUserIdColumn("create_by");
        return config;
    }

    private void createTable(Class<?> entity) {
        TableInfo table = TableInfoHelper.getTableInfo(entity);
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(table.getTableName())
                .append(" (id BIGINT PRIMARY KEY");
        table.getFieldList().forEach(field -> sql.append(", ").append(field.getColumn()).append(' ')
                .append(field.getPropertyType() == LocalDateTime.class ? "TIMESTAMP"
                        : Number.class.isAssignableFrom(field.getPropertyType()) ? "BIGINT" : "VARCHAR(2000)"));
        jdbc.execute(sql.append(')').toString());
    }

    private void seed() {
        jdbc.execute("INSERT INTO sys_resource (id,tenant_id,parent_id,client_code,del_flag,visible,menu_status,create_by) VALUES "
                + "(1,20,0,NULL,0,0,0,10),(2,20,1,'',0,0,0,10),(3,20,2,'pc',0,0,0,10),"
                + "(4,20,5,'pc',0,1,1,10),(5,20,0,'pc',5,1,1,10),(6,20,0,'h5',0,1,1,10),"
                + "(7,21,0,'pc',0,1,1,10),(8,21,0,NULL,0,1,1,10),(9,20,0,'pc',0,1,1,11),"
                + "(11,20,7,'pc',0,1,1,10)");
        jdbc.execute("INSERT INTO sys_role_resource (id,tenant_id,role_id,resource_id) VALUES "
                + "(10,20,7,9),(11,20,7,1),(12,20,7,9),(13,20,8,3),(14,21,7,6)");
    }

    @Override public void close() {
        try {
            session.close();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
