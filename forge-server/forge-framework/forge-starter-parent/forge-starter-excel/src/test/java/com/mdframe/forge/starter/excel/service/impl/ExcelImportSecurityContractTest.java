package com.mdframe.forge.starter.excel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.spi.ExcelMetadataProvider;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelImportSecurityContractTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void unexpectedImportFailureShouldReturnOnlyStablePublicMessage() {
        String internalMessage = "jdbc:mysql://db.internal/forge?password=secret";
        ExcelImportServiceImpl service = new ExcelImportServiceImpl();

        ImportResult<GenericRowData> result = service.importData(
                new ByteArrayInputStream(internalMessage.getBytes(StandardCharsets.UTF_8)),
                internalMessage,
                GenericRowData.class);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSummary())
                .isEqualTo(ImportResult.PUBLIC_FAILURE_MESSAGE)
                .doesNotContain(internalMessage);
    }

    @Test
    void explicitAllowImportFalseStillRejectsImportEnabledConfigType() {
        ExcelExportMetadata metadata = new ExcelExportMetadata();
        metadata.setConfigKey("disabled_import");
        metadata.setConfigType("BOTH");
        metadata.setStatus(1);
        metadata.setAllowImport(false);
        ExcelImportServiceImpl service = new ExcelImportServiceImpl();
        ExcelMetadataProvider metadataProvider = configKey -> metadata;
        ReflectionTestUtils.setField(service, "metadataProvider", metadataProvider);

        assertThatThrownBy(() -> service.downloadTemplate("disabled_import"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当前配置未开启导入");
    }

    @Test
    void listenerShouldNotExposeUnexpectedParserMessage() {
        String internalMessage = "Zip bomb detected at /private/upload.xlsx";
        ImportResult<GenericRowData> result = new ImportResult<>();
        GenericRowDataListener listener = new GenericRowDataListener(List.of(), result, null);

        listener.onException(new IllegalStateException(internalMessage), null);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage())
                .isEqualTo("文件内容解析失败")
                .doesNotContain(internalMessage);
    }

    @Test
    void errorReportShouldUseEnglishFixedDisplayAndPreserveBusinessValues() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);
        ImportErrorRecord error = new ImportErrorRecord();
        error.setRowNum(7);
        error.setColumnName("客户字段");
        error.setRawValue("ZS-RAW-001");
        error.setErrorType("业务校验");
        error.setErrorMessage("客户原始错误");
        error.setSuggestion("客户原始建议");
        String taskId = "bilingual-" + System.nanoTime();
        String reportPath = new ExcelImportServiceImpl().generateErrorReport(taskId, List.of(error));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(reportPath))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Import Error Report");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Row number");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue()).isEqualTo("Original value");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue()).isEqualTo("ZS-RAW-001");
        } finally {
            Files.deleteIfExists(Path.of(reportPath));
        }
    }

    @Test
    void importResultSerializationShouldHideInternalErrorReportPath() throws Exception {
        String internalPath = "/private/import/error-report.xlsx";
        ImportResult<Object> result = new ImportResult<>();
        result.setErrorReportPath(internalPath);

        assertThat(new ObjectMapper().writeValueAsString(result))
                .doesNotContain("errorReportPath", internalPath);
    }
}
