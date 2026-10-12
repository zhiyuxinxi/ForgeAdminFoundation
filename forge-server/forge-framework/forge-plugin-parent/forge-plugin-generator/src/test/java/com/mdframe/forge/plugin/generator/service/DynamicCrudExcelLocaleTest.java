package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudExportTask;
import com.mdframe.forge.starter.file.model.FileMetadata;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.mapper.AiCrudExportTaskMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DynamicCrudExcelLocaleTest {
    private final DynamicCrudService crud = mock(DynamicCrudService.class);
    private final AiCrudExportTaskMapper tasks = mock(AiCrudExportTaskMapper.class);
    private final FileManager files = mock(FileManager.class);
    @SuppressWarnings("unchecked")
    private final DynamicCrudExcelService service = new DynamicCrudExcelService(crud, new ObjectMapper(),
            mock(NamedParameterJdbcTemplate.class), mock(ObjectProvider.class), mock(ObjectProvider.class),
            tasks, files, mock(IDataScopeService.class));

    @AfterEach void reset() {
        LocaleContextHolder.resetLocaleContext();
        TenantContextHolder.clear();
    }

    @Test void exportedWorkbookUsesEnglishHeadersAndPreservesRecordText() throws Exception {
        TenantContextHolder.setTenantId(1L);
        LocaleContextHolder.setLocale(Locale.US);
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("locale-test");
        config.setAppName("原应用名");
        config.setModelSchema("{\"businessNameI18n\":{\"en-US\":\"Public records\"}}");
        config.setColumnsSchema("""
                [{"prop":"name","title":"姓名","titleI18n":{"en-US":"Name"}},
                 {"prop":"memo","title":"未配置英文的字段"}]
                """);
        when(crud.getRuntimeValues("locale-test")).thenReturn(RuntimeCrudConfigAdapter.fromDesign(config));
        when(crud.countExportRows(eq("locale-test"), any(), isNull())).thenReturn(1L);
        when(crud.selectExportPageRows(eq("locale-test"), any(), eq(1), eq(1), isNull()))
                .thenReturn(List.of(Map.of("name", "客户原始姓名", "memo", "不得翻译的备注")));
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.exportExcel("locale-test", new DynamicCrudQuery(), response);
        assertTrue(URLDecoder.decode(response.getHeader("Content-Disposition"), StandardCharsets.UTF_8).contains("Public records"));
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Data", sheet.getSheetName());
            assertEquals("Name", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("未配置英文的字段", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("客户原始姓名", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("不得翻译的备注", sheet.getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test void workerUsesCapturedLocaleAndRestoresThreadContextEvenOnFailure() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        TenantContextHolder.setTenantId(7L);
        var context = new DynamicCrudExcelService.ExportExecutionContext(12L, 1L, 1L, 1L,
                null, 1L, 100, 24, Locale.US);
        when(tasks.selectById(12L)).thenAnswer(invocation -> {
            assertEquals(Locale.US, LocaleContextHolder.getLocale());
            assertEquals(1L, TenantContextHolder.getTenantId());
            throw new IllegalStateException("simulated failure");
        });
        assertThrows(IllegalStateException.class, () -> service.executeAsyncExportTask(12L, "test", new DynamicCrudQuery(), context));
        assertEquals(Locale.SIMPLIFIED_CHINESE, LocaleContextHolder.getLocale());
        assertEquals(7L, TenantContextHolder.getTenantId());
    }

    @Test void asyncWorkerWritesEnglishWorkbookAndRestoresCallerLocale() throws Exception {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        TenantContextHolder.setTenantId(7L);
        AiCrudExportTask task = new AiCrudExportTask();
        task.setId(12L);
        task.setFileName("Records.xlsx");
        when(tasks.selectById(12L)).thenReturn(task);
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("async-locale");
        config.setColumnsSchema("[{\"prop\":\"name\",\"title\":\"名称\",\"titleI18n\":{\"en-US\":\"Name\"}}]");
        when(crud.getRuntimeValues("async-locale")).thenReturn(RuntimeCrudConfigAdapter.fromDesign(config));
        when(crud.selectExportPageRows(eq("async-locale"), any(), eq(1), eq(100), isNull()))
                .thenReturn(List.of(Map.of("name", "业务原文-preserved")));
        when(files.upload(any(InputStream.class), eq("Records.xlsx"), anyString(), anyString(), eq("12"), isNull(), eq(true), anyLong()))
                .thenAnswer(invocation -> {
                    assertEquals(Locale.US, LocaleContextHolder.getLocale());
                    try (var workbook = new XSSFWorkbook((InputStream) invocation.getArgument(0))) {
                        assertEquals("Data", workbook.getSheetName(0));
                        assertEquals("Name", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
                        assertEquals("业务原文-preserved", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
                    }
                    FileMetadata metadata = FileMetadata.builder().fileId("fixture-file").fileSize(100L).build();
                    return metadata;
                });
        service.executeAsyncExportTask(12L, "async-locale", new DynamicCrudQuery(),
                new DynamicCrudExcelService.ExportExecutionContext(12L,1L,1L,1L,null,1L,100,24,Locale.US));
        verify(files).upload(any(InputStream.class), eq("Records.xlsx"), anyString(), anyString(), eq("12"), isNull(), eq(true), anyLong());
        verify(tasks).updateById(argThat((AiCrudExportTask updated) -> "fixture-file".equals(updated.getFileId()) && Integer.valueOf(100).equals(updated.getProgress())));
        assertEquals(Locale.SIMPLIFIED_CHINESE, LocaleContextHolder.getLocale());
        assertEquals(7L, TenantContextHolder.getTenantId());
    }

    @Test void failedImportUsesEnglishValidationAndDoesNotWriteRecords() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);
        TenantContextHolder.setTenantId(1L);
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("locale-import");
        config.setEditSchema("""
                [{"field":"name","label":"姓名","labelI18n":{"en-US":"Name"},"required":true},
                 {"field":"memo","label":"备注","labelI18n":{"en-US":"Notes"}}]
                """);
        when(crud.getRuntimeValues("locale-import")).thenReturn(RuntimeCrudConfigAdapter.fromDesign(config));
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Input");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Notes");
            var row = sheet.createRow(1);
            row.createCell(1).setCellValue("客户原始备注");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        var result = service.importExcel("locale-import", new MockMultipartFile("file", "input.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes));
        assertFalse(result.getSuccess());
        assertEquals("Import validation errors: 1", result.getSummary());
        assertEquals("Name is required", result.getErrors().get(0).getMessage());
        verify(crud, never()).insert(anyString(), anyMap());
    }
}
