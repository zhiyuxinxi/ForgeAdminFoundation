package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GenTableColumnMapperQueryTest {
    private GenTableColumnMapper mapper;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:gen_columns_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE gen_table (table_id BIGINT PRIMARY KEY,table_name VARCHAR(100),"
                + "create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE gen_table_column (column_id BIGINT PRIMARY KEY,table_id BIGINT,"
                + "column_name VARCHAR(100),column_comment VARCHAR(100),column_comment_en VARCHAR(100),"
                + "column_type VARCHAR(100),java_type VARCHAR(100),java_field VARCHAR(100),"
                + "is_pk INT,is_increment INT,is_required INT,is_insert INT,is_edit INT,is_list INT,"
                + "is_query INT,query_type VARCHAR(30),html_type VARCHAR(30),dict_type VARCHAR(30),"
                + "dict_labels_i18n VARCHAR(1000),validate_rule VARCHAR(1000),formula_config VARCHAR(1000),"
                + "ai_recommended INT,sort INT,create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.update("INSERT INTO gen_table VALUES (10,'sample_record','2020-01-01 00:00:00',"
                + "'2020-01-02 00:00:00'),(20,'other_record',null,null)");
        jdbc.update("INSERT INTO gen_table_column(column_id,table_id,column_name,column_comment,"
                + "column_comment_en,sort,create_time,update_time) VALUES "
                + "(101,10,'title','标题','Title',2,'2026-10-15 01:00:00','2026-10-16 02:00:00'),"
                + "(102,10,'id','编号','ID',1,null,null),(201,20,'private_field','其他','Other',1,null,null)");

        MybatisPlusInterceptor chain = new MybatisPlusInterceptor();
        chain.addInnerInterceptor(new TenantLineInnerInterceptor(new DefaultTenantLineHandler(new TenantProperties())));
        chain.addInnerInterceptor(new DataScopeInterceptor(mock(IDataScopeService.class), new DataScopeProperties()));
        chain.addInnerInterceptor(new PaginationInnerInterceptor());
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setPlugins(chain);
        factory.setMapperLocations(new ClassPathResource("mapper/GenTableColumnMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(GenTableColumnMapper.class);
    }

    @Test
    void joinedLookupReturnsColumnMetadataInOrderWithoutAmbiguousSharedNames() {
        List<GenTableColumn> columns = mapper.selectConfiguredColumnsByTableName("sample_record");
        assertEquals(List.of(102L, 101L), columns.stream().map(GenTableColumn::getColumnId).toList());
        GenTableColumn title = columns.get(1);
        assertEquals(10L, title.getTableId());
        assertEquals("标题", title.getColumnComment());
        assertEquals("Title", title.getColumnCommentEn());
        assertEquals(LocalDateTime.of(2026, 10, 15, 1, 0), title.getCreateTime());
        assertEquals(LocalDateTime.of(2026, 10, 16, 2, 0), title.getUpdateTime());
    }

    @Test
    void sharedProjectionAlsoWorksForSingleTableAndKeepsTableNameParameterized() {
        assertEquals(List.of(102L, 101L), mapper.selectByTableId(10L).stream()
                .map(GenTableColumn::getColumnId).toList());
        assertEquals(List.of(201L), mapper.selectConfiguredColumnsByTableName("other_record").stream()
                .map(GenTableColumn::getColumnId).toList());
        assertTrue(mapper.selectConfiguredColumnsByTableName("sample_record' OR '1'='1").isEmpty());
    }
}
