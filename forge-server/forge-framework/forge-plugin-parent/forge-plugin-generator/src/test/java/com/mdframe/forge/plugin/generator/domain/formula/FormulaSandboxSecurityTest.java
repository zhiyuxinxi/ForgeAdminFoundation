package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.AviatorEvaluator;
import com.mdframe.forge.plugin.generator.service.formula.FormulaExecutionEngine;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionDefinition;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulaSandboxSecurityTest {
    private static final String MARKER = "forge.formula.sandbox.marker";

    @Test
    void preservesArithmeticRegisteredFunctionsDatesAndDependencies() {
        ExpressionExecutor executor = new ExpressionExecutor();
        assertEquals(27L, executor.execute("math.abs(price) * quantity + 3", Map.of("price", -8L, "quantity", 3L)));
        assertEquals("2026-10-09", executor.execute("date_to_string(day, 'yyyy-MM-dd')",
                Map.of("day", LocalDateTime.of(2026, 10, 9, 0, 0))));
        assertTrue(new AviatorAdapter().validate("price * quantity").isValid());
        assertEquals(java.util.List.of("price", "quantity"), new AviatorAdapter().extractVariables("price * quantity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "use java.lang.System; System.setProperty('forge.formula.sandbox.marker', 'changed')",
            "use java.lang.Math; Math.abs(-7)", "new java.lang.String('x')",
            "java.lang.System.setProperty('forge.formula.sandbox.marker', 'changed')",
            "require('io')", "load('anything')", "io.open('anything')", "eval('1+1')",
            "__instance__", "let x = 1; x", "while true { 1 }", "for x in seq.list(1) { x }",
            "lambda(x) -> x end", "getClass(value)", "__env__",
            "'aaa' =~ /(a+)+$/", "2N ** 8", "1N << 8"
    })
    void rejectsHostAndScriptCapabilitiesAtCompilationAndExecution(String expression) {
        String original = System.getProperty(MARKER);
        try {
            System.setProperty(MARKER, "unchanged");
            AviatorAdapter adapter = new AviatorAdapter();
            assertFalse(adapter.validate(expression).isValid(), expression);
            assertThrows(FormulaCompileException.class, () -> adapter.compile(expression), expression);
            assertThrows(FormulaExecutionException.class,
                    () -> new ExpressionExecutor().execute(expression, Map.of("value", "safe")), expression);
            assertEquals("unchanged", System.getProperty(MARKER));
        } finally {
            if (original == null) System.clearProperty(MARKER); else System.setProperty(MARKER, original);
        }
    }

    @Test
    void rejectsHostObjectsAndExternallyCompiledExpressions() {
        var compiled = new AviatorAdapter().compile("value");
        assertThrows(IllegalArgumentException.class, () -> compiled.execute(Map.of("value", System.class)));
        assertThrows(IllegalArgumentException.class, () -> compiled.execute(Map.of("value", Map.of("nested", new Object()))));
        assertThrows(FormulaExecutionException.class, () -> new ExpressionExecutor()
                .executeCompiled(AviatorEvaluator.compile("1+1"), Map.of()));
        assertEquals(3L, new ExpressionExecutor().executeCompiled(new AviatorAdapter().compile("1+2"), Map.of()));
    }

    @Test
    void publicationValidationAndRuntimeUseTheSameRestrictions() {
        var formula = FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.VIRTUAL)
                .expression("use java.lang.System; System.setProperty('forge.formula.sandbox.marker', 'changed')").build();
        var formulas = Map.of("result", formula);
        assertFalse(new FormulaValidationService().validate(formulas).isValid());
        assertFalse(new FormulaExecutionEngine().execute(formulas, Map.of()).isSuccess());
        var legal = Map.of("result", FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.VIRTUAL).expression("math.abs(-7)").build());
        assertTrue(new FormulaValidationService().validate(legal).isValid());
        assertEquals(7L, new FormulaExecutionEngine().execute(legal, Map.of()).getResults().get("result"));
    }

    @Test
    void lengthAndEnvironmentAreBounded() {
        AviatorAdapter adapter = new AviatorAdapter();
        assertThrows(FormulaCompileException.class, () -> adapter.compile("1+".repeat(3000) + "1"));
        assertThrows(IllegalArgumentException.class, () -> adapter.compile("value").execute(Map.of("value", "x".repeat(65537))));
        assertEquals("** =~ __env__", adapter.compile("'** =~ __env__'").execute());
        assertEquals(8.0, new ExpressionExecutor().execute("math.pow(2, 3)", Map.of()));
    }

    @Test
    void runtimeMetadataStaysOutsideExpressionsWithoutWeakeningEnvironmentRestrictions() {
        FormulaExecutionEngine engine = new FormulaExecutionEngine();
        FormulaRuntimeContext runtimeContext = new FormulaRuntimeContext(1L, "default", "testObj", Map.of());
        Map<String, Object> environment = new java.util.LinkedHashMap<>();
        environment.put("price", 10L);
        environment.put("quantity", 5L);
        environment.put("__formulaRuntimeContext__", runtimeContext);
        var calc = FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.STORED)
                .expression("price * quantity").build();
        var conditional = FormulaConfig.builder().type(FormulaType.CONDITIONAL).mode(FormulaMode.STORED)
                .condition(new ConditionConfig("price > 0", 1L, 0L)).build();

        var result = engine.execute(Map.of("total", calc, "positive", conditional), environment);
        assertTrue(result.isSuccess());
        assertEquals(50L, result.getResults().get("total"));
        assertEquals(1L, result.getResults().get("positive"));
        assertSame(runtimeContext, environment.get("__formulaRuntimeContext__"));

        Map<String, Object> reservedKey = new java.util.LinkedHashMap<>(environment);
        reservedKey.put("__other__", "not allowed");
        assertFalse(engine.execute(Map.of("total", calc), reservedKey).isSuccess());
        Map<String, Object> hostObject = new java.util.LinkedHashMap<>(environment);
        hostObject.put("host", new Object());
        assertFalse(engine.execute(Map.of("total", calc), hostObject).isSuccess());
        Map<String, Object> untrustedMetadata = new java.util.LinkedHashMap<>(environment);
        untrustedMetadata.put("__formulaRuntimeContext__", "not a runtime context");
        assertFalse(engine.execute(Map.of("total", calc), untrustedMetadata).isSuccess());
        var internalReference = FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.STORED)
                .expression("__formulaRuntimeContext__").build();
        assertFalse(engine.execute(Map.of("internal", internalReference), environment).isSuccess());
    }

    @Test
    void compilationCacheIsBounded() throws Exception {
        AviatorAdapter adapter = new AviatorAdapter();
        for (int i = 0; i < AviatorAdapter.MAX_CACHE_ENTRIES + 20; i++) adapter.compile("value + " + i);
        var field = AviatorAdapter.class.getDeclaredField("evaluator");
        field.setAccessible(true);
        var instance = (com.googlecode.aviator.AviatorEvaluatorInstance) field.get(adapter);
        assertTrue(instance.getExpressionCacheSize() <= AviatorAdapter.MAX_CACHE_ENTRIES);
    }

    @Test
    void disablingRegisteredFunctionCannotUseAnEarlierCachedExpression() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        AviatorAdapter adapter = new AviatorAdapter(registry);
        var compiled = adapter.compile("math.abs(-7)");
        registry.registerDefinition(FormulaFunctionDefinition.builder().functionCode("math.abs")
                .status(FormulaFunctionDefinition.STATUS_DISABLED).beanName("formulaBuiltinFunctionProvider")
                .methodName("mathAbs").returnType("NUMBER").argument("value", "NUMBER", true).build());
        assertThrows(RuntimeException.class, compiled::execute);
        assertFalse(adapter.validate("math.abs(-7)").isValid());
        assertEquals(7L, new ExpressionExecutor().execute("math.abs(-7)", Map.of()));
    }
}
