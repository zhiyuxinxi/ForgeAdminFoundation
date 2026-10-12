package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.AviatorAdapter;
import com.mdframe.forge.plugin.generator.domain.formula.ExpressionExecutor;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaFunctionRegistry")
@Tag("dev")
class FormulaFunctionRegistryTest {

    @Test
    @DisplayName("lists enabled builtin functions")
    void listsEnabledBuiltinFunctions() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();

        assertTrue(registry.listEnabledResponses().size() > 10);
        assertTrue(registry.find("math.abs").isPresent());
        assertTrue(registry.find("string.contains").isPresent());
        assertTrue(registry.find("date_to_string").isPresent());
    }

    @Test
    @DisplayName("registered Java Bean function executes through Aviator")
    void registeredFunctionExecutesThroughAviator() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        ExpressionExecutor executor = new ExpressionExecutor(new AviatorAdapter(), registry);

        Object result = executor.execute("string.contains(name, 'ell')", Map.of("name", "hello"));

        assertEquals(Boolean.TRUE, result);
    }

    @Test
    @DisplayName("argument schema rejects wrong arity and type")
    void argumentSchemaRejectsInvalidCalls() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();

        assertThrows(IllegalArgumentException.class,
            () -> registry.invoke("math.max", new Object[] {3L}));
        assertThrows(IllegalArgumentException.class,
            () -> registry.invoke("string.length", new Object[] {123L}));
    }

    @Test
    @DisplayName("disabled function is rejected by validation")
    void disabledFunctionRejectedByValidation() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        registry.registerDefinition(FormulaFunctionDefinition.builder()
            .functionCode("string.contains")
            .displayName("包含判断")
            .category("String")
            .description("disabled for test")
            .returnType("BOOLEAN")
            .status(FormulaFunctionDefinition.STATUS_DISABLED)
            .beanName("formulaBuiltinFunctionProvider")
            .methodName("stringContains")
            .argument("value", "STRING", true)
            .argument("keyword", "STRING", true)
            .build());

        List<String> errors = registry.validateFunctionReferences(
            "string.contains(name, 'x')", List.of("string.contains"));

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("disabled"));
    }

    @Test
    @DisplayName("execution wraps function argument failures")
    void executionWrapsFunctionArgumentFailures() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        ExpressionExecutor executor = new ExpressionExecutor(new AviatorAdapter(), registry);

        assertThrows(FormulaExecutionException.class,
            () -> executor.execute("math.max(1)", Map.of()));
    }

    @Test
    @DisplayName("function invocation honors configured timeout")
    void functionInvocationHonorsConfiguredTimeout() {
        FormulaRuntimeProperties properties = new FormulaRuntimeProperties();
        properties.setFunctionTimeoutMs(1L);
        FormulaFunctionInvoker invoker = new FormulaFunctionInvoker(
            Map.of("slowBean", new SlowFormulaFunctionBean()), properties);
        FormulaFunctionDefinition definition = FormulaFunctionDefinition.builder()
            .functionCode("test.slow")
            .displayName("slow")
            .category("Test")
            .description("slow")
            .returnType("STRING")
            .beanName("slowBean")
            .methodName("slow")
            .timeoutMs(1000L)
            .build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> invoker.invoke(definition, new Object[0]));
        assertTrue(error.getMessage().contains("timed out"));
        assertTrue(error.getMessage().contains("timeoutMs=1"));
    }

    public static class SlowFormulaFunctionBean {
        public String slow() throws InterruptedException {
            Thread.sleep(10L);
            return "done";
        }
    }
}
