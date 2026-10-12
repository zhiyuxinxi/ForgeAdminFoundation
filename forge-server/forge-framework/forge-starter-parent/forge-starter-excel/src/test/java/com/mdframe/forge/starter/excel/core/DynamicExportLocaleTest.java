package com.mdframe.forge.starter.excel.core;

import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.excel.spi.ExcelMetadataProvider;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicExportLocaleTest {
    public static class Rows {
        public List<Row> list() {
            return List.of(new Row());
        }
    }

    public static class Row {
        public String getUsername() { return "张三-keep-原文"; }
    }

    @AfterEach void resetLocale() { LocaleContextHolder.resetLocaleContext(); }

    private DynamicExportEngine engine() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean("rows")).thenReturn(new Rows());
        ExcelExportMetadata metadata = new ExcelExportMetadata();
        metadata.setStatus(1);
        metadata.setConfigType("EXPORT");
        metadata.setExportName("人员列表");
        metadata.setSheetName("人员列表");
        metadata.setExportNameEn("Public records");
        metadata.setSheetNameEn("Public records");
        metadata.setFileNameTemplateEn("Public_records_{date}.xlsx");
        metadata.setFileNameTemplate("人员列表_{date}.xlsx");
        metadata.setDataSourceBean("rows");
        metadata.setQueryMethod("list");
        metadata.setAutoTrans(false);
        ExcelMetadataProvider metadataProvider = mock(ExcelMetadataProvider.class);
        when(metadataProvider.getMetadata("people")).thenReturn(metadata);
        ExcelColumnConfig column = new ExcelColumnConfig();
        column.setFieldName("username");
        column.setColumnName("用户名");
        column.setColumnNameEn("User name");
        column.setExport(true);
        column.setOrderNum(1);
        ExcelConfigProvider configProvider = mock(ExcelConfigProvider.class);
        when(configProvider.getColumnConfigs("people")).thenReturn(List.of(column));
        DynamicExportEngine engine = new DynamicExportEngine(context);
        ReflectionTestUtils.setField(engine, "metadataProvider", metadataProvider);
        ReflectionTestUtils.setField(engine, "configProvider", configProvider);
        return engine;
    }

    @Test void exportsHttpWorkbookAndFilenameInEnglishWithoutTranslatingRecords() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);
        MockHttpServletResponse response = new MockHttpServletResponse();
        engine().export(response, "people", Map.of());
        assertTrue(URLDecoder.decode(response.getHeader("Content-disposition"), StandardCharsets.UTF_8)
                .matches(".*Public_records_\\d{8}\\.xlsx"));
        assertWorkbook(response.getContentAsByteArray(), "Public records", "User name");
    }

    @Test void streamConsumerHonorsTheWorkerLocaleAndKeepsChineseDefault() throws Exception {
        DynamicExportEngine engine = engine();
        for (Locale locale : List.of(Locale.US, Locale.SIMPLIFIED_CHINESE)) {
            LocaleContextHolder.setLocale(locale);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            engine.exportToStream(output, "people", Map.of());
            assertWorkbook(output.toByteArray(), locale.equals(Locale.US) ? "Public records" : "人员列表",
                    locale.equals(Locale.US) ? "User name" : "用户名");
        }
    }

    private void assertWorkbook(byte[] bytes, String sheetName, String header) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(sheetName, workbook.getSheetName(0));
            assertEquals(header, workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("张三-keep-原文", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        }
    }
}
