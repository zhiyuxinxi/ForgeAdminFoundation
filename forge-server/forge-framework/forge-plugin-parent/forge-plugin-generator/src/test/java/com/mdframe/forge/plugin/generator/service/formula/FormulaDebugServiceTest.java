package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDebugRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDebugResponse;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaDebugService")
@Tag("dev")
class FormulaDebugServiceTest {

    @Test
    @DisplayName("single field debug includes formula dependency closure")
    void singleFieldDebugIncludesDependencyClosure() {
        CapturingLogService logService = new CapturingLogService();
        FormulaDebugService service = service(logService);
        FormulaDebugRequest request = new FormulaDebugRequest();
        request.setObjectCode("order");
        request.setRecordId("1001");
        request.setFieldCode("grandTotal");
        request.setSampleValues(Map.of("price", 100L, "quantity", 2L, "freight", 10L));
        request.setFormulas(List.of(
                calc("subtotal", "price * quantity", List.of("price", "quantity")),
                calc("grandTotal", "subtotal + freight", List.of("subtotal", "freight"))
        ));

        FormulaDebugResponse response = service.debug(request);

        assertTrue(response.isSuccess(), "Errors: " + response.getErrors());
        assertNotNull(response.getTraceId());
        assertEquals(List.of("subtotal", "grandTotal"), response.getExecutionPlan());
        assertEquals(210L, response.getResult().get("grandTotal"));
        assertEquals(2, response.getSteps().size());
        assertEquals(2, logService.records.size());
        assertEquals(response.getTraceId(), logService.records.get(0).getTraceId());
    }

    @Test
    @DisplayName("conditional debug returns branch metadata")
    void conditionalDebugReturnsBranchMetadata() {
        FormulaDebugService service = service(new CapturingLogService());
        FormulaDebugRequest request = new FormulaDebugRequest();
        request.setSampleValues(Map.of("amount", 1500L));
        request.setFormulas(List.of(conditional("level", "amount > 1000", "VIP", "NORMAL")));

        FormulaDebugResponse response = service.debug(request);

        assertTrue(response.isSuccess(), "Errors: " + response.getErrors());
        assertEquals("VIP", response.getResult().get("level"));
        assertEquals(true, response.getSteps().get(0).getMetadata().get("conditionMatched"));
        assertEquals("VIP", response.getSteps().get(0).getMetadata().get("trueValue"));
        assertEquals("NORMAL", response.getSteps().get(0).getMetadata().get("falseValue"));
    }

    @Test
    @DisplayName("failed debug still returns trace id and records failed log")
    void failedDebugReturnsTraceId() {
        CapturingLogService logService = new CapturingLogService();
        FormulaDebugService service = service(logService);
        FormulaDebugRequest request = new FormulaDebugRequest();
        request.setObjectCode("order");
        request.setRecordId("1001");
        request.setSampleValues(Map.of("price", 100L));
        request.setFormulas(List.of(calc("badField", "price *", List.of("price"))));

        FormulaDebugResponse response = service.debug(request);

        assertFalse(response.isSuccess());
        assertNotNull(response.getTraceId());
        assertTrue(response.getErrors().containsKey("badField"));
        assertEquals(1, logService.records.size());
        assertEquals(Boolean.FALSE, logService.records.get(0).getSuccess());
    }

    private FormulaDebugService service(CapturingLogService logService) {
        return new FormulaDebugService(new FormulaExecutionEngine(), logService, new ObjectMapper());
    }

    private FormulaDebugRequest.FormulaFieldConfig calc(String fieldCode,
                                                        String expression,
                                                        List<String> dependsOn) {
        FormulaDebugRequest.FormulaFieldConfig field = new FormulaDebugRequest.FormulaFieldConfig();
        field.setFieldCode(fieldCode);
        field.setType("CALC");
        field.setExpression(expression);
        field.setDependsOn(dependsOn);
        return field;
    }

    private FormulaDebugRequest.FormulaFieldConfig conditional(String fieldCode,
                                                               String expression,
                                                               Object trueValue,
                                                               Object falseValue) {
        FormulaDebugRequest.FormulaFieldConfig field = new FormulaDebugRequest.FormulaFieldConfig();
        field.setFieldCode(fieldCode);
        field.setType("CONDITIONAL");
        FormulaDebugRequest.ConditionDebug condition = new FormulaDebugRequest.ConditionDebug();
        condition.setExpression(expression);
        condition.setTrueValue(trueValue);
        condition.setFalseValue(falseValue);
        field.setCondition(condition);
        return field;
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
