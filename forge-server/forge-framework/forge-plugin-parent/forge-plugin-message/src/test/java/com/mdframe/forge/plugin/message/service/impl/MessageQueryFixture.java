package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Runs the real XML, tenant, data-scope and pagination interceptors against an isolated database. */
final class MessageQueryFixture implements AutoCloseable {
    final IDataScopeService scopes = mock(IDataScopeService.class);
    final JdbcTemplate jdbc;
    final SqlSession session;
    final MessageManageServiceImpl management;
    final MessageTemplateServiceImpl templates;

    MessageQueryFixture() throws IOException {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:message_query_" + UUID.randomUUID() + ";MODE=MySQL");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
        MybatisPlusInterceptor interceptors = new MybatisPlusInterceptor();
        interceptors.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public LongValue getTenantId() { return new LongValue(20); }
        }));
        DataScopeProperties properties = new DataScopeProperties();
        properties.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        interceptors.addInnerInterceptor(new DataScopeInterceptor(scopes, properties));
        interceptors.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptors);
        when(scopes.getDataScopeConfig(anyString())).thenAnswer(call -> {
            String id = call.getArgument(0);
            if (!id.endsWith(".selectList") && !id.endsWith(".selectCount")
                    && !id.endsWith(".selectById")) {
                return null;
            }
            SysDataScopeConfig config = new SysDataScopeConfig();
            config.setEnabled(0);
            return config;
        });
        for (Class<?> mapper : new Class<?>[] {
                SysMessageMapper.class, SysMessageTemplateMapper.class, SysMessageSendRecordMapper.class}) {
            Path xml = Path.of(System.getProperty("message.query.resources"), mapper.getSimpleName() + ".xml");
            if (Files.exists(xml)) {
                try (var input = Files.newInputStream(xml)) {
                    new XMLMapperBuilder(input, configuration, xml.toString(), configuration.getSqlFragments()).parse();
                }
            } else {
                configuration.addMapper(mapper);
            }
        }
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        jdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                session.getConnection(), true));
        createTable(SysMessage.class);
        createTable(SysMessageTemplate.class);
        createTable(SysMessageSendRecord.class);
        management = new MessageManageServiceImpl(session.getMapper(SysMessageMapper.class),
                mock(SysMessageReceiverMapper.class), session.getMapper(SysMessageSendRecordMapper.class));
        templates = new MessageTemplateServiceImpl(session.getMapper(SysMessageTemplateMapper.class));
    }

    void restrictToCreator() {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setEnabled(1);
        config.setResourceCode("message-query-test");
        config.setUserIdColumn("create_by");
        when(scopes.getDataScopeConfig(SysMessageMapper.class.getName() + ".selectList")).thenReturn(config);
        when(scopes.getDataScopeConfig(SysMessageTemplateMapper.class.getName() + ".selectList")).thenReturn(config);
        DataScopeContext context = new DataScopeContext();
        context.setUserId(10L);
        context.setTenantId(20L);
        context.setMinDataScope(5);
        when(scopes.getCurrentUserDataScope("message-query-test")).thenReturn(context);
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

    @Override public void close() { session.close(); }
}
