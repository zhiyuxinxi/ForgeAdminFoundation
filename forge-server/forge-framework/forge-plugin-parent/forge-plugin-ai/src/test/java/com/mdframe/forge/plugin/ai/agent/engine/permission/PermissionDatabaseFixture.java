package com.mdframe.forge.plugin.ai.agent.engine.permission;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolContributor;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import com.mdframe.forge.plugin.ai.agenttool.mapper.AiAgentToolPermissionMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import java.io.InputStream;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** An actual MyBatis XML query over an isolated H2 schema, with only tool effects replaced. */
final class PermissionDatabaseFixture implements AutoCloseable {
    final SqlSession session;
    final ReactContext context = new ReactContext();
    final AgentTool tool = tool("lookup");
    final AgentTool risky = tool("delete_item");
    final AgentTool rag = tool("rag_search");
    final AgentTool other = tool("lookup");
    final PermissionEngine engine;

    PermissionDatabaseFixture() throws Exception {
        var dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        String resource = "mapper/AiAgentToolPermissionMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        schema();
        context.setAgentId(1L);
        context.setTenantId(20L);
        context.setUserId(10L);
        context.setSessionId("session");
        context.setBoundTools(List.of(tool, risky, rag));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(
                contributor("builtin", List.of(tool, risky, rag)), contributor("mcp", List.of(other))));
        engine = new PermissionEngine(new AgentToolPermissionService(session.getMapper(AiAgentToolPermissionMapper.class)), registry);
    }

    private void schema() throws Exception {
        sql("CREATE TABLE ai_agent (id BIGINT, tenant_id BIGINT, del_flag BIGINT, knowledge_ids VARCHAR, rag_mode VARCHAR)");
        sql("CREATE TABLE ai_chat_session (id VARCHAR, user_id BIGINT, tenant_id BIGINT, status VARCHAR)");
        sql("CREATE TABLE ai_agent_tool_config (tenant_id BIGINT, agent_id BIGINT, del_flag BIGINT, enabled VARCHAR, tool_source VARCHAR, tool_key VARCHAR)");
        sql("CREATE TABLE ai_agent_tool_permission (id BIGINT, tenant_id BIGINT, agent_id BIGINT, tool_key VARCHAR, decision VARCHAR, del_flag BIGINT)");
        sql("INSERT INTO ai_agent VALUES (1,20,0,'[3]',NULL), (2,21,0,NULL,NULL)");
        sql("INSERT INTO ai_chat_session VALUES ('session',10,20,'0')");
        sql("INSERT INTO ai_agent_tool_config VALUES (20,1,0,'1','builtin','lookup'), (20,1,0,'1','builtin','delete_item')");
    }

    void sql(String statement) throws Exception {
        try (Statement command = session.getConnection().createStatement()) {
            command.execute(statement);
        }
    }

    void policy(String decision) throws Exception {
        sql("DELETE FROM ai_agent_tool_permission");
        try (var statement = session.getConnection().prepareStatement(
                "INSERT INTO ai_agent_tool_permission VALUES (1,20,1,'lookup',?,0)")) {
            statement.setString(1, decision);
            statement.executeUpdate();
        }
    }

    private static AgentTool tool(String key) {
        AgentTool tool = mock(AgentTool.class);
        when(tool.getKey()).thenReturn(key);
        when(tool.execute(any(), any())).thenReturn(ToolResult.text("ok"));
        return tool;
    }

    private static AgentToolContributor contributor(String source, List<AgentTool> tools) {
        AgentToolContributor contributor = mock(AgentToolContributor.class);
        when(contributor.getSource()).thenReturn(source);
        when(contributor.contribute()).thenReturn(tools);
        return contributor;
    }

    @Override
    public void close() {
        session.close();
    }
}
