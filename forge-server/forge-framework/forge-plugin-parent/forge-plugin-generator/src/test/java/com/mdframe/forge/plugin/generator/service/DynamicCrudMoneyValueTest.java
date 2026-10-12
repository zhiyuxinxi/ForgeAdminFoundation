package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeMoneyValues;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicCrudMoneyValueTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void convertsMajorUnitInputToMinorUnitStorage() throws Exception {
        RuntimeMoneyValues service = service();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cash_amount", "12.30");

        service.store(data, config());

        assertEquals(1230L, data.get("cash_amount"));
    }

    @Test
    void convertsMinorUnitStorageToMajorUnitDisplay() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cash_amount", 1230L);

        new RuntimeMoneyValues(new RuntimeFieldMetadata(objectMapper)).money(List.of(row), config());

        assertEquals(new BigDecimal("12.30"), row.get("cash_amount"));
    }

    @Test
    void rejectsMoneyInputWithMoreFractionalDigitsThanConfigured() {
        RuntimeMoneyValues service = service();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cash_amount", "12.345");

        assertThrows(BusinessException.class, () -> service.store(data, config()));
    }

    @Test
    void rejectsMoneyInputBelowConfiguredMinimum() {
        RuntimeMoneyValues service = service();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cash_amount", "0.00");

        assertThrows(BusinessException.class, () -> service.store(data, config()));
    }

    @Test
    void leavesDecimalMoneyFieldsUntouchedForLegacyCompatibility() throws Exception {
        RuntimeMoneyValues service = service();
        LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
        config.setModelSchema("""
                {"fields":[{"field":"amount","columnName":"amount","dataType":"decimal","componentType":"money","precision":2}]}
                """);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", "12.30");

        service.store(data, config);

        assertEquals("12.30", data.get("amount"));
    }

    private RuntimeMoneyValues service() {
        return new RuntimeMoneyValues(new RuntimeFieldMetadata(objectMapper));
    }

    private LowcodeRuntimeConfig config() {
        LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
        config.setModelSchema("""
                {"fields":[{"field":"cashAmount","columnName":"cash_amount","dataType":"bigint","componentType":"money","businessFieldType":"MONEY","precision":2,"basicProps":{"min":0.01}}]}
                """);
        return config;
    }
}
