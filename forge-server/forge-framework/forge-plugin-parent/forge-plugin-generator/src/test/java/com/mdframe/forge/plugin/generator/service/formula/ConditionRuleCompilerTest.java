package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.ConditionRuleNode;
import com.mdframe.forge.plugin.generator.dto.formula.ConditionRuleCompileRequest;
import com.mdframe.forge.plugin.generator.dto.formula.ConditionRuleCompileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConditionRuleCompiler")
@Tag("dev")
class ConditionRuleCompilerTest {

    private final ConditionRuleCompiler compiler = new ConditionRuleCompiler();

    @Test
    @DisplayName("compile AND group to Aviator expression")
    void compileAndGroup() {
        ConditionRuleCompileRequest request = request(
            ConditionRuleNode.group("AND", List.of(
                ConditionRuleNode.condition("amount", "GT", 1000),
                ConditionRuleNode.condition("status", "EQ", "ACTIVE")
            ))
        );
        request.setFields(List.of(field("amount", "decimal"), field("status", "varchar")));

        ConditionRuleCompileResponse response = compiler.compile(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertEquals("(amount > 1000 && status == 'ACTIVE')", response.getExpression());
        assertEquals(List.of("amount", "status"), response.getDependencies());
    }

    @Test
    @DisplayName("empty group returns clear error")
    void emptyGroup() {
        ConditionRuleCompileResponse response = compiler.compile(request(
            ConditionRuleNode.group("AND", List.of())
        ));

        assertFalse(response.isValid());
        assertTrue(response.getErrors().get(0).contains("分组不能为空"));
    }

    @Test
    @DisplayName("illegal operator returns clear error")
    void illegalOperator() {
        ConditionRuleCompileResponse response = compiler.compile(request(
            ConditionRuleNode.condition("amount", "BAD", 1000)
        ));

        assertFalse(response.isValid());
        assertTrue(response.getErrors().get(0).contains("不支持的条件操作符"));
    }

    @Test
    @DisplayName("missing field returns clear error")
    void missingField() {
        ConditionRuleCompileResponse response = compiler.compile(request(
            ConditionRuleNode.condition("", "EQ", "ACTIVE")
        ));

        assertFalse(response.isValid());
        assertTrue(response.getErrors().get(0).contains("条件字段不能为空"));
    }

    @Test
    @DisplayName("unknown field is rejected when field metadata exists")
    void unknownField() {
        ConditionRuleCompileRequest request = request(ConditionRuleNode.condition("missing", "EQ", "A"));
        request.setFields(List.of(field("status", "varchar")));

        ConditionRuleCompileResponse response = compiler.validate(request);

        assertFalse(response.isValid());
        assertTrue(response.getErrors().get(0).contains("条件字段不存在"));
    }

    @Test
    @DisplayName("numeric field rejects text value")
    void numericValueType() {
        ConditionRuleCompileRequest request = request(ConditionRuleNode.condition("amount", "GT", "1000"));
        request.setFields(List.of(field("amount", "decimal")));

        ConditionRuleCompileResponse response = compiler.validate(request);

        assertFalse(response.isValid());
        assertTrue(response.getErrors().get(0).contains("需要数字类型"));
    }

    @Test
    @DisplayName("IN compiles to disjunction and requires list value")
    void inOperator() {
        ConditionRuleCompileRequest request = request(ConditionRuleNode.condition("status", "IN", List.of("A", "B")));
        request.setFields(List.of(field("status", "varchar")));

        ConditionRuleCompileResponse response = compiler.compile(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertEquals("(status == 'A' || status == 'B')", response.getExpression());

        ConditionRuleCompileResponse invalid = compiler.validate(request(
            ConditionRuleNode.condition("status", "IN", "A")
        ));
        assertFalse(invalid.isValid());
        assertTrue(invalid.getErrors().get(0).contains("需要集合类型"));
    }

    @Test
    @DisplayName("null operators do not require value")
    void nullOperators() {
        ConditionRuleCompileResponse response = compiler.compile(request(
            ConditionRuleNode.condition("ownerId", "NOT_NULL", null)
        ));

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertEquals("ownerId != nil", response.getExpression());
    }

    private ConditionRuleCompileRequest request(ConditionRuleNode rule) {
        ConditionRuleCompileRequest request = new ConditionRuleCompileRequest();
        request.setRule(rule);
        return request;
    }

    private ConditionRuleCompileRequest.FieldDefinition field(String fieldCode, String dataType) {
        ConditionRuleCompileRequest.FieldDefinition field = new ConditionRuleCompileRequest.FieldDefinition();
        field.setFieldCode(fieldCode);
        field.setDataType(dataType);
        return field;
    }
}
