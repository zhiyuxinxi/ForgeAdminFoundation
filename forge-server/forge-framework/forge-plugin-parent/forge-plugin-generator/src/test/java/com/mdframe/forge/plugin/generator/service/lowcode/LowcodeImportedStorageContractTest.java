package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelImportRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LowcodeImportedStorageContractTest {
    @Test
    void createModelKeepsDefaultGeneratedIdAndAuditFields() {
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setTableMode("CREATE");
        new LowcodeModelSchemaNormalizer().normalizeModelFields(schema);
        assertTrue(schema.getFields().get(0).getAutoIncrement());
        assertEquals("id", schema.getFields().get(0).getColumnName());
        assertTrue(schema.getFields().stream().anyMatch(field -> "tenant_id".equals(field.getColumnName())));
    }

    @Test
    void importedAssignedIdSurvivesNormalizationWithoutInventingPhysicalColumns() {
        LowcodeModelSchema schema = preview("id", "varchar(32)", 0, "varchar(32)");
        new LowcodeModelSchemaNormalizer().normalizeModelFields(schema);
        new LowcodeSchemaValidator().validateModel(schema);
        assertEquals(List.of("id", "name"), schema.getFields().stream().map(field -> field.getColumnName()).toList());
        assertFalse(schema.getPrimaryKey().getAutoIncrement());
        assertTrue(schema.getFields().get(0).getPrimaryKey());
        assertFalse(schema.getFields().get(0).getAutoIncrement());
        assertTrue(schema.getFields().get(0).getFormVisible());
        assertFalse(schema.getFields().get(0).getReadonly());
        assertEquals("PRIMARY_KEY", schema.getLogicDeleteStrategy().getDeletedValueMode());
    }

    @Test
    void importedNumericPrimaryKeyUsesBigintTombstonesButTinyintKeepsFixedFlag() {
        assertEquals("PRIMARY_KEY", preview("record_id", "bigint", 1, "bigint")
                .getLogicDeleteStrategy().getDeletedValueMode());
        LowcodeModelSchema ordinary = preview("record_id", "bigint", 1, "tinyint");
        assertEquals("VALUE", ordinary.getLogicDeleteStrategy().getDeletedValueMode());
        assertEquals("1", ordinary.getLogicDeleteStrategy().getDeletedValue());
        assertFalse(ordinary.getFields().get(0).getFormVisible());
        assertTrue(ordinary.getFields().get(0).getReadonly());
        assertEquals("VALUE", preview("record_code", "varchar(64)", 0, "varchar(32)")
                .getLogicDeleteStrategy().getDeletedValueMode());
    }

    private LowcodeModelSchema preview(String keyName, String keyType, int generated, String deleteType) {
        IGenDatasourceService sources = mock(IGenDatasourceService.class);
        LowcodeDomainService domains = mock(LowcodeDomainService.class);
        LowcodeRuntimeDataSourceResolver resolver = mock(LowcodeRuntimeDataSourceResolver.class);
        GenDatasource source = new GenDatasource();
        source.setDatasourceId(7L);
        source.setIsEnabled(1);
        AiLowcodeDomain domain = new AiLowcodeDomain();
        domain.setId(3L);
        domain.setDomainCode("example");
        GenTable table = new GenTable();
        table.setTableName("biz_imported");
        when(domains.requireEnabledDomain(3L)).thenReturn(domain);
        when(sources.getById(7L)).thenReturn(source);
        when(sources.selectDbTableByName(7L, "biz_imported")).thenReturn(table);
        when(sources.selectDbTableColumnsByName(7L, "biz_imported")).thenReturn(List.of(
                column(keyName, keyType, 1, generated), column("name", "varchar(50)", 0, 0),
                column("del_flag", deleteType, 0, 0)));
        LowcodeModelImportRequest request = new LowcodeModelImportRequest();
        request.setDomainId(3L);
        request.setDatasourceId(7L);
        request.setTableName("biz_imported");
        request.setModelName("Imported");
        return new LowcodeModelImportService(sources, domains, null, resolver).previewDbTableModel(request);
    }

    private GenTableColumn column(String name, String type, int primary, int generated) {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(name);
        column.setColumnType(type);
        column.setIsPk(primary);
        column.setIsIncrement(generated);
        return column;
    }
}
