package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Feature;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.lexer.ExpressionLexer;
import com.googlecode.aviator.lexer.token.Token;
import com.googlecode.aviator.exception.CompileExpressionErrorException;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry;
import com.mdframe.forge.starter.core.i18n.UiMessages;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin adapter over the Aviator expression engine.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Compile expression strings into executable {@link Expression} objects</li>
 *   <li>Extract variable (dependency) names from expressions</li>
 *   <li>Parse error messages to extract human-readable error positions</li>
 * </ul>
 * <p>
 * Does NOT execute expressions with real data — that stays in Phase 2B.
 * This adapter is deliberately narrow; only compile-time features are exposed.
 */
public class AviatorAdapter {

    /** Regex to extract line:column from Aviator error messages. */
    private static final Pattern POSITION_PATTERN =
        Pattern.compile("at\\s+\\((?:(\\d+):(\\d+))\\)");

    public static final int MAX_EXPRESSION_LENGTH = 4096;
    public static final int MAX_CACHE_ENTRIES = 256;
    private final AviatorEvaluatorInstance evaluator = AviatorEvaluator.newInstance();
    private FormulaFunctionRegistry functionRegistry;

    public AviatorAdapter() {
        this(FormulaFunctionRegistry.builtin());
    }

    public AviatorAdapter(FormulaFunctionRegistry functionRegistry) {
        this.functionRegistry = Objects.requireNonNull(functionRegistry);
        evaluator.enableSandboxMode();
        // 5.4.3's sandbox alone leaves the io module accessible. Remove all modules,
        // script features and engine functions; only the formula registry is callable.
        evaluator.getModuleCache().clear();
        evaluator.setOption(Options.FEATURE_SET, EnumSet.noneOf(Feature.class));
        evaluator.setOption(Options.ENABLE_PROPERTY_SYNTAX_SUGAR, false);
        evaluator.setOption(Options.OPTIMIZE_LEVEL, AviatorEvaluator.EVAL);
        evaluator.setOption(Options.EVAL_TIMEOUT_MS, 1000L);
        evaluator.getFuncMap().clear();
        evaluator.useLRUExpressionCache(MAX_CACHE_ENTRIES);
    }

    public synchronized void useFunctionRegistry(FormulaFunctionRegistry functionRegistry) {
        if (this.functionRegistry != functionRegistry) {
            this.functionRegistry = Objects.requireNonNull(functionRegistry);
            evaluator.getFuncMap().clear();
            evaluator.clearExpressionCache();
        }
    }

    /**
     * Compile an expression string. Returns the compiled expression on success,
     * or throws {@link FormulaCompileException} with position details on failure.
     *
     * @param expression raw Aviator expression string
     * @return compiled Expression ready for execution
     * @throws FormulaCompileException if syntax is invalid
     */
    public synchronized Expression compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new FormulaCompileException("Expression must not be blank", null, -1, -1);
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new FormulaCompileException(UiMessages.label("公式长度超过上限", "Formula exceeds the length limit"), null, -1, -1);
        }
        try {
            validateResourceBounds(expression);
            functionRegistry.registerAviatorFunctions(evaluator);
            Expression compiled = evaluator.compile(expression, true);
            if (compiled.getVariableFullNames().stream().anyMatch(name -> name.startsWith("__"))) {
                throw new FormulaCompileException(UiMessages.label("公式不允许访问内部变量", "Internal variables are not allowed in formulas"), null, -1, -1);
            }
            for (String function : compiled.getFunctionNames()) {
                if (!functionRegistry.find(function).filter(definition -> definition.isEnabled() && definition.isJavaBean()).isPresent()) {
                    throw new FormulaCompileException(UiMessages.label("公式函数未获准: ", "Formula function is not allowed: ") + function, null, -1, -1);
                }
            }
            return new RestrictedExpression(compiled, functionRegistry);
        } catch (ExpressionSyntaxErrorException e) {
            throw FormulaCompileException.fromSyntaxError(e);
        } catch (CompileExpressionErrorException e) {
            throw FormulaCompileException.fromCompileError(e);
        } catch (Exception e) {
            // Catch-all for unexpected Aviator errors
            throw new FormulaCompileException(
                "Unexpected compile error: " + e.getMessage(), e, -1, -1);
        }
    }

    /**
     * Extract all variable names referenced in an expression.
     * <p>
     * Uses Aviator's built-in {@code getVariableNames()} after compilation.
     * This is the authoritative way to get dependencies — more reliable than regex.
     *
     * @param expression raw Aviator expression string
     * @return sorted list of variable names (never null)
     */
    public List<String> extractVariables(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Expression compiled = compile(expression);
            List<String> vars = new ArrayList<>(compiled.getVariableNames());
            Collections.sort(vars);
            return Collections.unmodifiableList(vars);
        } catch (Exception e) {
            // If expression is invalid, fall back to empty list;
            // validation will catch the error separately.
            return Collections.emptyList();
        }
    }

    /**
     * Validate expression syntax and return a structured result.
     * <p>
     * This is the primary entry point for syntax-only validation.
     * Does not check dependency correctness — that is ExpressionParser's job.
     *
     * @param expression raw Aviator expression string
     * @return validation result with success flag, error details, and variable list
     */
    public SyntaxValidationResult validate(String expression) {
        if (expression == null || expression.isBlank()) {
            return SyntaxValidationResult.error("Expression must not be blank", -1, -1);
        }
        try {
            Expression compiled = compile(expression);
            List<String> vars = new ArrayList<>(compiled.getVariableNames());
            Collections.sort(vars);
            return SyntaxValidationResult.success(vars);
        } catch (ExpressionSyntaxErrorException e) {
            return FormulaCompileException.fromSyntaxError(e).toValidationResult();
        } catch (CompileExpressionErrorException e) {
            return FormulaCompileException.fromCompileError(e).toValidationResult();
        } catch (Exception e) {
            return SyntaxValidationResult.error("Unexpected error: " + e.getMessage(), -1, -1);
        }
    }

    // ─── inner types ───

    private void validateResourceBounds(String expression) {
        ExpressionLexer lexer = new ExpressionLexer(evaluator, expression);
        Token<?> token;
        String previous = "";
        int tokens = 0;
        int depth = 0;
        while ((token = lexer.scan()) != null) {
            if (++tokens > 1024) throw resourceLimit();
            if (token.getType() == Token.TokenType.Number) safeValue(token.getValue(Map.of()));
            if (token.getType() != Token.TokenType.Char) {
                previous = "";
                continue;
            }
            String current = token.getLexeme();
            if (current.equals("~") || current.equals("*") && previous.equals("*")
                    || current.equals("<") && previous.equals("<") || current.equals(">") && previous.equals(">")) {
                // Native regex/exponent/shift operations can exceed the engine budget in Java.
                // Business exponent calculations use the bounded numeric math.pow function.
                throw resourceLimit();
            }
            if (current.equals("(") || current.equals("[")) {
                if (++depth > 64) throw resourceLimit();
            } else if (current.equals(")") || current.equals("]")) depth--;
            previous = current;
        }
    }

    private static IllegalArgumentException resourceLimit() {
        return new IllegalArgumentException(UiMessages.label("公式计算超出受限表达式范围", "Formula exceeds the restricted expression limits"));
    }

    /** Copy only bounded data values, never expose a bean, Class or reflection object. */
    public static Object safeValue(Object value) {
        return FormulaSandboxValues.copy(value);
    }

    public static boolean isRestricted(Expression expression) {
        return expression instanceof RestrictedExpression;
    }

    private static final class RestrictedExpression implements Expression {
        private final Expression delegate;
        private final FormulaFunctionRegistry registry;

        private RestrictedExpression(Expression delegate, FormulaFunctionRegistry registry) {
            this.delegate = delegate;
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object execute(Map<String, Object> env) {
            for (String name : delegate.getFunctionNames()) {
                if (registry.find(name).filter(definition -> definition.isEnabled() && definition.isJavaBean()).isEmpty()) {
                    throw new IllegalArgumentException(UiMessages.label("公式函数未获准: ", "Formula function is not allowed: ") + name);
                }
            }
            return safeValue(delegate.execute((Map<String, Object>) safeValue(env == null ? Map.of() : env)));
        }

        @Override public Object execute() { return execute(Map.of()); }
        @Override public String getSourceFile() { return delegate.getSourceFile(); }
        @Override public List<String> getVariableNames() { return delegate.getVariableNames(); }
        @Override public List<String> getVariableFullNames() { return delegate.getVariableFullNames(); }
        @Override public Map<String, Object> newEnv(Object... args) { return delegate.newEnv(args); }
        @Override public String addSymbol(String symbol) { return delegate.addSymbol(symbol); }
        @Override public List<String> getFunctionNames() { return delegate.getFunctionNames(); }
    }

    /**
     * Structured result of expression syntax validation.
     */
    public static class SyntaxValidationResult {
        private final boolean valid;
        private final List<String> variables;
        private final String errorMessage;
        private final int errorLine;
        private final int errorColumn;

        private SyntaxValidationResult(boolean valid, List<String> variables,
                                        String errorMessage, int errorLine, int errorColumn) {
            this.valid = valid;
            this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
            this.errorMessage = errorMessage;
            this.errorLine = errorLine;
            this.errorColumn = errorColumn;
        }

        static SyntaxValidationResult success(List<String> variables) {
            return new SyntaxValidationResult(true, variables, null, -1, -1);
        }

        static SyntaxValidationResult error(String message, int line, int column) {
            return new SyntaxValidationResult(false, Collections.emptyList(), message, line, column);
        }

        public boolean isValid() { return valid; }
        public List<String> getVariables() { return variables; }
        public String getErrorMessage() { return errorMessage; }
        public int getErrorLine() { return errorLine; }
        public int getErrorColumn() { return errorColumn; }
        public boolean hasPosition() { return errorLine >= 0 && errorColumn >= 0; }

        @Override
        public String toString() {
            if (valid) return "SyntaxValidationResult{valid=true, vars=" + variables + "}";
            return "SyntaxValidationResult{valid=false, error='" + errorMessage
                + "', pos=(" + errorLine + ":" + errorColumn + ")}";
        }
    }
}
