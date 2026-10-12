package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("人员和组织选择值校验")
class SelectionIdentifierValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("人员和组织 bigint 主字段只接受数字 ID")
    void bigintSelectionFieldsOnlyAcceptIdentifiers() {
        LowcodeRuntimeConfig config = config();

        assertDoesNotThrow(() -> SelectionIdentifierValidator.validateRuntime(config, Map.of(
                "applicantId", "1900000000000000001",
                "departmentId", 1900000000000000002L,
                "applicantIdName", "张三",
                "departmentIdName", "研发部"
        ), objectMapper));

        BusinessException userError = assertThrows(BusinessException.class,
                () -> SelectionIdentifierValidator.validateRuntime(config, Map.of("applicantId", "张三"), objectMapper));
        assertEquals("申请人必须保存人员ID，不能保存姓名", userError.getMessage());

        BusinessException orgError = assertThrows(BusinessException.class,
                () -> SelectionIdentifierValidator.validateRuntime(config, Map.of("departmentId", "研发部"), objectMapper));
        assertEquals("所属部门必须保存组织ID，不能保存名称", orgError.getMessage());
    }

    private LowcodeRuntimeConfig config() {
        LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
        config.setConfigKey("hr_apply");
        config.setEditSchema("""
                [
                  {"field":"applicantId","label":"申请人","type":"userSelect"},
                  {"field":"departmentId","label":"所属部门","type":"orgTreeSelect"}
                ]
                """);
        config.setModelSchema("""
                {"fields":[
                  {"field":"applicantId","columnName":"applicant_id","label":"申请人","dataType":"bigint","componentType":"userSelect"},
                  {"field":"departmentId","columnName":"department_id","label":"所属部门","dataType":"bigint","componentType":"orgTreeSelect"},
                  {"field":"applicantIdName","columnName":"applicant_id_name","label":"申请人姓名","dataType":"varchar","componentType":"input"},
                  {"field":"departmentIdName","columnName":"department_id_name","label":"部门名称","dataType":"varchar","componentType":"input"}
                ]}
                """);
        return config;
    }
}
