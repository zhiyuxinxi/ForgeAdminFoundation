package com.mdframe.forge.starter.excel.service.impl;

import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExcelImportLocaleTest {
    @AfterEach void resetLocale() { LocaleContextHolder.resetLocaleContext(); }

    private ExcelImportServiceImpl service() {
        ExcelColumnConfig name = new ExcelColumnConfig();
        name.setFieldName("name"); name.setColumnName("姓名"); name.setColumnNameEn("Full name");
        name.setImportable(true); name.setRequired(true); name.setExampleValue("张三-keep-原文"); name.setOrderNum(1);
        ExcelColumnConfig note = new ExcelColumnConfig();
        note.setFieldName("note"); note.setColumnName("备注"); note.setColumnNameEn("Notes");
        note.setImportable(true); note.setRequired(false); note.setExampleValue("user supplied text"); note.setOrderNum(2);
        ExcelConfigProvider provider = mock(ExcelConfigProvider.class);
        when(provider.getColumnConfigs("people")).thenReturn(List.of(name,note));
        ExcelImportServiceImpl service = new ExcelImportServiceImpl();
        ReflectionTestUtils.setField(service,"configProvider",provider);
        return service;
    }

    @Test void englishTemplateMustRoundTripAfterSwitchingToChineseWithoutChangingData() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);
        ExcelImportServiceImpl service = service();
        byte[] bytes = service.downloadTemplate("people");
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(book.getSheetAt(0).getSheetName()).isEqualTo("Import data");
            assertThat(book.getSheetAt(1).getSheetName()).isEqualTo("Instructions");
            assertThat(book.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Full name");
            assertThat(book.getSheetAt(1).getRow(1).getCell(5).getStringCellValue()).contains("Required.","Full name","Delete or replace");
        }
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        ImportResult<GenericRowData> imported=service.importData(new ByteArrayInputStream(bytes),"people",GenericRowData.class);
        assertThat(imported.isSuccess()).isTrue();
        assertThat(imported.getSuccessData()).hasSize(1);
        assertThat(imported.getSuccessData().get(0).getFields()).containsEntry("name","张三-keep-原文");
        assertThat(imported.getSummary()).isEqualTo("导入成功，共1行，成功1行");
    }

    @Test void chineseTemplateMustImportInEnglishAndValidationMustRemainEnglish() throws Exception {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        ExcelImportServiceImpl service=service();byte[] bytes=service.downloadTemplate("people");
        LocaleContextHolder.setLocale(Locale.US);
        assertThat(service.importData(new ByteArrayInputStream(bytes),"people",GenericRowData.class).isSuccess()).isTrue();
        ByteArrayOutputStream invalid=new ByteArrayOutputStream();
        try (XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            book.getSheetAt(0).getRow(1).getCell(0).setBlank();book.write(invalid);
        }
        ImportResult<GenericRowData> imported=service.importData(new ByteArrayInputStream(invalid.toByteArray()),"people",GenericRowData.class);
        assertThat(imported.isSuccess()).isFalse();assertThat(imported.getSuccessData()).isEmpty();
        assertThat(imported.getErrors().get(0).getErrorMessage()).isEqualTo("Full name is required");
        assertThat(imported.getSummary()).contains("failed","Full name is required");
    }
}
