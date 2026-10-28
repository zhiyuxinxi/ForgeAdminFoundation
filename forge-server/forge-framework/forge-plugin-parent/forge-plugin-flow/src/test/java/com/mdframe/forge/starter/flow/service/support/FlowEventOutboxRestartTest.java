package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.flow.event.FlowEventOutboxEntry;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowEventOutboxRestartTest {
    @TempDir
    Path directory;

    @Test
    void committedEventSurvivesDatabaseShutdownAndNewConnection() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("outbox").toAbsolutePath();
        var first = new DriverManagerDataSource(url, "sa", "");
        var firstJdbc = new JdbcTemplate(first);
        String migration = FlowTerminalOutboxFixture.readMigration("V1.0.2026100909__flow_terminal_event_outbox.sql");
        firstJdbc.execute(migration.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        var mapper = mapper(first);
        var entry = new FlowEventOutboxEntry("restart-event", 97L, "instance-before-restart", "PROCESS_COMPLETED",
                "approval", "redis", "flow:event:all", "{\"eventId\":\"restart-event\"}",
                LocalDateTime.parse("2026-10-09T04:00:00.123"));
        new TransactionTemplate(new DataSourceTransactionManager(first)).executeWithoutResult(status -> mapper.insert(entry));
        firstJdbc.execute("SHUTDOWN");

        var reopened = new DriverManagerDataSource(url, "sa", "");
        try {
            assertEquals(entry, mapper(reopened).selectRecorded(97L, "restart-event"));
        } finally {
            new JdbcTemplate(reopened).execute("SHUTDOWN");
        }
    }

    private FlowEventOutboxMapper mapper(DataSource dataSource) throws Exception {
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setMapperLocations(new ClassPathResource("mapper/FlowEventOutboxMapper.xml"));
        return new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventOutboxMapper.class);
    }
}
