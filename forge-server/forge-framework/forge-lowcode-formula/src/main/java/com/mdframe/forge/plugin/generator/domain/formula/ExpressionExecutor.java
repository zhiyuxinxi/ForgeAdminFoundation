package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.Expression;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry;
import com.mdframe.forge.starter.core.i18n.UiMessages;

import java.util.Map;
import java.util.Objects;

/**
 * Executes a single compiled Aviator expression with a variable context.
 * <p>
 * Thin wrapper over {@link AviatorAdapter} that handles execution-time concerns:
 * <ul>
 *   <li>Resolves variables from the provided context map</li>
 *   <li>Returns the computed value (Object)</li>
 *   <li>Wraps execution exceptions with field-level context</li>
 * </ul>
 * <p>
 * Pure domain logic — no Spring, no DB.
 */
public class ExpressionExecutor {

    private final AviatorAdapter aviator;
    private final FormulaFunctionRegistry functionRegistry;

    public ExpressionExecutor() {
        this(new AviatorAdapter(), FormulaFunctionRegistry.builtin());
    }

    public ExpressionExecutor(AviatorAdapter aviator) {
        this(aviator, FormulaFunctionRegistry.builtin());
    }

    public ExpressionExecutor(AviatorAdapter aviator, FormulaFunctionRegistry functionRegistry) {
        this.aviator = Objects.requireNonNull(aviator, "aviator must not be null");
        this.functionRegistry = Objects.requireNonNull(functionRegistry, "functionRegistry must not be null");
        this.aviator.useFunctionRegistry(functionRegistry);
    }

    /**
     * Compile and execute an expression with the given variable context.
     * <p>
     * This is the primary entry point. Compilation is done on each call
     * to ensure fresh expression state. For hot-path optimization in Phase 3B+,
     * pre-compiled expressions can be cached.
     *
     * @param expression raw Aviator expression string
     * @param variables  variable name → value map (field values + previously computed formulas)
     * @return the computed value (may be null for error degradation)
     * @throws FormulaExecutionException if compilation or execution fails
     */
    public Object execute(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            throw new FormulaExecutionException("Expression must not be blank", null);
        }
        Objects.requireNonNull(variables, "variables must not be null");

        try {
            Expression compiled = aviator.compile(expression);
            return compiled.execute(variables);
        } catch (FormulaCompileException e) {
            throw new FormulaExecutionException(
                "Compile error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FormulaExecutionException(
                "Execution error: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a pre-compiled expression (for cached/hot-path use).
     *
     * @param compiled  pre-compiled Aviator Expression
     * @param variables variable context
     * @return computed value
     * @throws FormulaExecutionException on execution failure
     */
    public Object executeCompiled(Expression compiled, Map<String, Object> variables) {
        Objects.requireNonNull(compiled, "compiled must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        try {
            if (!AviatorAdapter.isRestricted(compiled)) {
                throw new IllegalArgumentException(UiMessages.label("表达式必须由受限公式引擎编译", "Expression must be compiled by the restricted formula evaluator"));
            }
            return compiled.execute(variables);
        } catch (Exception e) {
            throw new FormulaExecutionException(
                "Execution error: " + e.getMessage(), e);
        }
    }
}
