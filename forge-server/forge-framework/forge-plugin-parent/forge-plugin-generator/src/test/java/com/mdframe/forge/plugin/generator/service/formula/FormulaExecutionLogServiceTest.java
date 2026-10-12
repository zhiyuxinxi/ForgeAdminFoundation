package com.mdframe.forge.plugin.generator.service.formula;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaExecutionLog;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogQueryDTO;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import com.mdframe.forge.plugin.generator.mapper.FormulaExecutionLogMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("FormulaExecutionLogService")
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class FormulaExecutionLogServiceTest {

    @Mock
    private FormulaExecutionLogMapper formulaExecutionLogMapper;

    @Test
    @DisplayName("record success log with masked snapshots")
    void recordSuccessLog() {
        FormulaExecutionLogService service = new FormulaExecutionLogService(formulaExecutionLogMapper);

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            sessionHelper.when(SessionHelper::getUserId).thenReturn(99L);
            sessionHelper.when(SessionHelper::getMainOrgId).thenReturn(88L);

            service.record(FormulaExecutionLogResponse.builder()
                    .traceId("trace-1")
                    .objectCode("crm_order")
                    .recordId("1001")
                    .fieldCode("totalAmount")
                    .formulaType("CALC")
                    .formulaMode("STORED")
                    .expression("price * quantity")
                    .inputSnapshot("{\"mobile\":\"13812345678\",\"price\":100}")
                    .outputValue("13812345678")
                    .success(true)
                    .elapsedMs(12L)
                    .build());
        }

        ArgumentCaptor<AiFormulaExecutionLog> captor = ArgumentCaptor.forClass(AiFormulaExecutionLog.class);
        verify(formulaExecutionLogMapper).insert(captor.capture());

        AiFormulaExecutionLog entity = captor.getValue();
        assertEquals(7L, entity.getTenantId());
        assertEquals("crm_order", entity.getObjectCode());
        assertEquals("totalAmount", entity.getFieldCode());
        assertEquals(Boolean.TRUE, entity.getSuccess());
        assertEquals(99L, entity.getCreateBy());
        assertEquals(88L, entity.getCreateDept());
        assertNotNull(entity.getCreateTime());
        assertEquals("{\"mobile\":\"****\",\"price\":100}", entity.getInputSnapshot());
        assertEquals("138****5678", entity.getOutputValue());
    }

    @Test
    @DisplayName("record failed log defaults success to false")
    void recordFailedLog() {
        FormulaExecutionLogService service = new FormulaExecutionLogService(formulaExecutionLogMapper);

        service.record(FormulaExecutionLogResponse.builder()
                .tenantId(3L)
                .objectCode("crm_order")
                .fieldCode("badField")
                .errorMessage("表达式错误")
                .build());

        ArgumentCaptor<AiFormulaExecutionLog> captor = ArgumentCaptor.forClass(AiFormulaExecutionLog.class);
        verify(formulaExecutionLogMapper).insert(captor.capture());

        AiFormulaExecutionLog entity = captor.getValue();
        assertEquals(3L, entity.getTenantId());
        assertFalse(entity.getSuccess());
        assertEquals("表达式错误", entity.getErrorMessage());
    }

    @Test
    @DisplayName("record is skipped when execution log disabled")
    void recordSkippedWhenExecutionLogDisabled() {
        FormulaRuntimeProperties properties = new FormulaRuntimeProperties();
        properties.setExecutionLogEnabled(false);
        FormulaExecutionLogService service = new FormulaExecutionLogService(
                formulaExecutionLogMapper, properties, new FormulaValueMasker());

        service.record(FormulaExecutionLogResponse.builder()
                .traceId("trace-disabled")
                .objectCode("crm_order")
                .fieldCode("totalAmount")
                .success(false)
                .build());

        verifyNoInteractions(formulaExecutionLogMapper);
    }

    @Test
    @DisplayName("page filters by current tenant and maps response")
    void page() {
        FormulaExecutionLogService service = new FormulaExecutionLogService(formulaExecutionLogMapper);
        FormulaExecutionLogQueryDTO query = new FormulaExecutionLogQueryDTO();
        query.setPageNum(2);
        query.setPageSize(20);
        query.setObjectCode("crm_order");
        query.setFieldCode("totalAmount");
        query.setSuccess(true);
        query.setTraceId("trace-1");
        query.setBeginTime(LocalDateTime.now().minusDays(1));
        query.setEndTime(LocalDateTime.now());

        AiFormulaExecutionLog entity = new AiFormulaExecutionLog();
        entity.setId(10L);
        entity.setTenantId(7L);
        entity.setTraceId("trace-1");
        entity.setObjectCode("crm_order");
        entity.setRecordId("1001");
        entity.setFieldCode("totalAmount");
        entity.setSuccess(true);

        Page<AiFormulaExecutionLog> mapperPage = new Page<>(2, 20);
        mapperPage.setRecords(List.of(entity));
        mapperPage.setTotal(1);

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            when(formulaExecutionLogMapper.selectFormulaExecutionLogPage(any(), eq(7L), eq(query)))
                    .thenReturn(mapperPage);

            Page<FormulaExecutionLogResponse> result = service.page(query);

            assertEquals(1, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getId());
            assertEquals("totalAmount", result.getRecords().get(0).getFieldCode());
        }
    }

    @Test
    @DisplayName("detail filters by current tenant")
    void detail() {
        FormulaExecutionLogService service = new FormulaExecutionLogService(formulaExecutionLogMapper);
        AiFormulaExecutionLog entity = new AiFormulaExecutionLog();
        entity.setId(10L);
        entity.setTenantId(7L);
        entity.setObjectCode("crm_order");
        entity.setFieldCode("totalAmount");

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            when(formulaExecutionLogMapper.selectFormulaExecutionLogById(7L, 10L)).thenReturn(entity);

            FormulaExecutionLogResponse response = service.detail(10L);

            assertEquals(10L, response.getId());
            assertEquals(7L, response.getTenantId());
            assertEquals("crm_order", response.getObjectCode());
        }

        verify(formulaExecutionLogMapper).selectFormulaExecutionLogById(7L, 10L);
    }
}
