package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Formula runtime trace logging")
@Tag("dev")
class FormulaRuntimeTraceLoggingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("failed stored formula records execution log")
    void failedStoredFormulaRecordsExecutionLog() {
        CapturingLogService logService = new CapturingLogService();
        StoredFormulaRuntime runtime = new StoredFormulaRuntime(
                new FormulaExecutionEngine(), objectMapper, logService);
        LowcodeModelSchema schema = schemaWithFields(List.of(
                formulaField("total", "missingValue + 1", List.of("missingValue"), FormulaMode.STORED)));

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1001L);

        runtime.calculate(List.of(record), schema, context());

        assertEquals(1, logService.records.size());
        FormulaExecutionLogResponse log = logService.records.get(0);
        assertEquals("testObj", log.getObjectCode());
        assertEquals("1001", log.getRecordId());
        assertEquals("total", log.getFieldCode());
        assertFalse(log.getSuccess());
        assertNotNull(log.getTraceId());
        assertNotNull(log.getErrorMessage());
    }

    @Test
    @DisplayName("debug mode records successful stored formula log")
    void debugModeRecordsSuccessfulStoredFormulaLog() {
        CapturingLogService logService = new CapturingLogService();
        StoredFormulaRuntime runtime = new StoredFormulaRuntime(
                new FormulaExecutionEngine(), objectMapper, logService);
        LowcodeModelSchema schema = schemaWithFields(List.of(
                formulaField("total", "price * quantity", List.of("price", "quantity"), FormulaMode.STORED)));

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1002L);
        record.put("price", 10L);
        record.put("quantity", 5L);

        runtime.calculate(List.of(record), schema, context(), FormulaTraceOptions.debug());

        assertEquals(1, logService.records.size());
        FormulaExecutionLogResponse log = logService.records.get(0);
        assertTrue(log.getSuccess());
        assertEquals("total", log.getFieldCode());
        assertTrue(log.getInputSnapshot().contains("\"price\":10"));
        assertEquals("50", log.getOutputValue());
    }

    @Test
    @DisplayName("debug mode records successful virtual formula log")
    void debugModeRecordsSuccessfulVirtualFormulaLog() {
        CapturingLogService logService = new CapturingLogService();
        VirtualFormulaRuntime runtime = new VirtualFormulaRuntime(
                new FormulaExecutionEngine(), objectMapper, logService);
        LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"),
                formulaField("taxedPrice", "price * 1.1", List.of("price"), FormulaMode.VIRTUAL)));

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 42L);
        record.put("price", 100L);

        runtime.calculate(List.of(record), schema, context(), FormulaTraceOptions.debug());

        assertEquals(1, logService.records.size());
        FormulaExecutionLogResponse log = logService.records.get(0);
        assertEquals("VIRTUAL", log.getFormulaMode());
        assertEquals("taxedPrice", log.getFieldCode());
        assertTrue(log.getSuccess());
        assertNotNull(log.getTraceId());
        assertTrue(log.getInputSnapshot().contains("\"price\":100"));
    }

    private LowcodeModelSchema schemaWithFields(List<LowcodeFieldSchema> fields) {
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setFields(fields);
        return schema;
    }

    private LowcodeFieldSchema plainField(String name) {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(name);
        field.setLabel(name);
        return field;
    }

    private LowcodeFieldSchema formulaField(String name,
                                            String expression,
                                            List<String> dependsOn,
                                            FormulaMode mode) {
        LowcodeFieldSchema field = plainField(name);
        Map<String, Object> formulaConfig = new LinkedHashMap<>();
        formulaConfig.put("type", "CALC");
        formulaConfig.put("mode", mode.name());
        formulaConfig.put("expression", expression);
        formulaConfig.put("dependsOn", dependsOn);
        field.setFormulaConfig(formulaConfig);
        return field;
    }

    private FormulaRuntimeContext context() {
        return new FormulaRuntimeContext(1L, "default", "testObj", Map.of());
    }

    private static class CapturingLogService extends FormulaExecutionLogService {
        private final List<FormulaExecutionLogResponse> records = new ArrayList<>();

        CapturingLogService() {
            super(null);
        }

        @Override
        public void record(FormulaExecutionLogResponse log) {
            records.add(log);
        }
    }
}
