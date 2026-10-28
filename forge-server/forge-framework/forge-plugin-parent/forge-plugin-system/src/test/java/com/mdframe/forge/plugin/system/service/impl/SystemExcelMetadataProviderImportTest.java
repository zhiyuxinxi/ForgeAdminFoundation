package com.mdframe.forge.plugin.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import com.mdframe.forge.plugin.system.mapper.SysExcelExportConfigMapper;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.service.impl.ExcelImportServiceImpl;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemExcelMetadataProviderImportTest {

    @Test
    void explicitAllowImportSurvivesSystemMetadataConversionAndEnablesExportConfigurationImport() {
        SysExcelExportConfigMapper mapper = mock(SysExcelExportConfigMapper.class);
        SysExcelExportConfig config = new SysExcelExportConfig();
        config.setConfigKey("example_export");
        config.setConfigType("EXPORT");
        config.setStatus(1);
        config.setAllowImport(true);
        when(mapper.selectByConfigKey("example_export")).thenReturn(config);

        ExcelColumnConfig idCard = new ExcelColumnConfig();
        idCard.setFieldName("idCard");
        idCard.setColumnName("身份证号");
        idCard.setOrderNum(1);
        idCard.setImportable(true);
        idCard.setRequired(true);

        ExcelImportServiceImpl importService = new ExcelImportServiceImpl();
        ExcelConfigProvider configProvider = configKey -> List.of(idCard);
        ReflectionTestUtils.setField(importService, "metadataProvider", new SystemExcelMetadataProvider(mapper));
        ReflectionTestUtils.setField(importService, "configProvider", configProvider);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output)
                .head(List.of(List.of("身份证号")))
                .sheet("人防管理")
                .doWrite(List.of(List.of("11010519491231002X")));

        ImportResult<GenericRowData> result = importService.importData(
                new ByteArrayInputStream(output.toByteArray()),
                "example_export",
                GenericRowData.class);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isZero();
        assertThat(result.getSuccessData()).singleElement()
                .extracting(row -> row.getField("idCard"))
                .isEqualTo("11010519491231002X");
    }
}
