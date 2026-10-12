package com.mdframe.forge.plugin.generator.service.formula;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaSandboxValues;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry for formula functions available to validation, execution, and UI discovery.
 */
@Component
public class FormulaFunctionRegistry {

    private static final Pattern FUNCTION_CALL_PATTERN =
        Pattern.compile("(?<![A-Za-z0-9_.$])([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*\\(");

    private final FormulaFunctionInvoker invoker;
    private final Map<String, FormulaFunctionDefinition> definitions = new LinkedHashMap<>();

    @Autowired
    public FormulaFunctionRegistry(FormulaFunctionInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker must not be null");
        registerBuiltinDefinitions();
    }

    public static FormulaFunctionRegistry builtin() {
        return new FormulaFunctionRegistry(new FormulaFunctionInvoker(
            Map.of("formulaBuiltinFunctionProvider", new FormulaBuiltinFunctionProvider())));
    }

    public void registerAviatorFunctions(AviatorEvaluatorInstance evaluator) {
        for (FormulaFunctionDefinition definition : definitions.values()) {
            if (definition.isEnabled()
                && definition.isJavaBean()) {
                if (!evaluator.containsFunction(definition.getFunctionCode())) {
                    evaluator.addFunction(new RegisteredAviatorFunction(definition.getFunctionCode(), this));
                }
            } else {
                evaluator.removeFunction(definition.getFunctionCode());
            }
        }
    }

    public void registerDefinition(FormulaFunctionDefinition definition) {
        definitions.put(definition.getFunctionCode(), definition);
    }

    public Optional<FormulaFunctionDefinition> find(String functionCode) {
        if (functionCode == null || functionCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(functionCode));
    }

    public List<FormulaFunctionDefinition> listDefinitions() {
        return definitions.values().stream()
            .sorted(Comparator.comparing(FormulaFunctionDefinition::getCategory)
                .thenComparing(FormulaFunctionDefinition::getFunctionCode))
            .toList();
    }

    public List<FormulaFunctionResponse> listEnabledResponses() {
        return listDefinitions().stream()
            .filter(FormulaFunctionDefinition::isEnabled)
            .map(this::toResponse)
            .toList();
    }

    public List<String> extractFunctionCodes(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> codes = new LinkedHashSet<>();
        Matcher matcher = FUNCTION_CALL_PATTERN.matcher(expression);
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return new ArrayList<>(codes);
    }

    public List<String> validateFunctionReferences(String expression, Collection<String> explicitRefs) {
        Set<String> functionCodes = new LinkedHashSet<>(extractFunctionCodes(expression));
        if (explicitRefs != null) {
            functionCodes.addAll(explicitRefs);
        }

        List<String> errors = new ArrayList<>();
        for (String functionCode : functionCodes) {
            FormulaFunctionDefinition definition = definitions.get(functionCode);
            if (definition == null) {
                if (isManagedFunctionName(functionCode)) {
                    errors.add("Formula function is not registered: " + functionCode);
                }
                continue;
            }
            if (!definition.isEnabled()) {
                errors.add("Formula function is disabled: " + functionCode);
            }
            if (!definition.isJavaBean()) {
                errors.add("Formula function implementation is not supported: " + functionCode);
            }
        }
        return errors;
    }

    public Object invoke(String functionCode, Object[] args) {
        FormulaFunctionDefinition definition = definitions.get(functionCode);
        if (definition == null) {
            throw new IllegalArgumentException("Formula function is not registered: " + functionCode);
        }
        return invoker.invoke(definition, args);
    }

    private FormulaFunctionResponse toResponse(FormulaFunctionDefinition definition) {
        return FormulaFunctionResponse.builder()
            .name(definition.getFunctionCode())
            .displayName(definition.getDisplayName())
            .category(definition.getCategory())
            .description(definition.getDescription())
            .argumentSchema(toArgumentSchema(definition.getArguments()))
            .returnType(definition.getReturnType())
            .sourceType(definition.getSourceType())
            .example(definition.getExample())
            .build();
    }

    private String toArgumentSchema(List<FormulaFunctionDefinition.ArgumentDefinition> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < arguments.size(); i++) {
            FormulaFunctionDefinition.ArgumentDefinition argument = arguments.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"name\":\"").append(escapeJson(argument.name())).append("\",")
                .append("\"type\":\"").append(escapeJson(argument.type())).append("\",")
                .append("\"required\":").append(argument.required()).append("}");
        }
        return builder.append("]").toString();
    }

    private String escapeJson(String value) {
        return String.valueOf(value == null ? "" : value)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    boolean isManagedFunctionName(String functionCode) {
        return functionCode.contains(".") || "date_to_string".equals(functionCode);
    }

    private void registerBuiltinDefinitions() {
        builtin("math.abs", "绝对值", "Math", "返回数字绝对值", "NUMBER",
            "math.abs(-5) => 5", "mathAbs", false, arg("value", "NUMBER"));
        builtin("math.round", "四舍五入", "Math", "返回最接近的整数", "NUMBER",
            "math.round(3.6) => 4", "mathRound", false, arg("value", "NUMBER"));
        builtin("math.floor", "向下取整", "Math", "返回不大于入参的最大整数", "NUMBER",
            "math.floor(3.9) => 3", "mathFloor", false, arg("value", "NUMBER"));
        builtin("math.ceil", "向上取整", "Math", "返回不小于入参的最小整数", "NUMBER",
            "math.ceil(3.1) => 4", "mathCeil", false, arg("value", "NUMBER"));
        builtin("math.max", "最大值", "Math", "返回两个数字中的最大值", "NUMBER",
            "math.max(3, 7) => 7", "mathMax", false, arg("left", "NUMBER"), arg("right", "NUMBER"));
        builtin("math.min", "最小值", "Math", "返回两个数字中的最小值", "NUMBER",
            "math.min(3, 7) => 3", "mathMin", false, arg("left", "NUMBER"), arg("right", "NUMBER"));
        builtin("math.pow", "幂运算", "Math", "返回 left 的 right 次方", "NUMBER",
            "math.pow(2, 3) => 8", "mathPow", false, arg("left", "NUMBER"), arg("right", "NUMBER"));
        builtin("math.sqrt", "平方根", "Math", "返回数字平方根", "NUMBER",
            "math.sqrt(16) => 4", "mathSqrt", false, arg("value", "NUMBER"));
        builtin("string.length", "字符串长度", "String", "返回字符串长度", "NUMBER",
            "string.length('hello') => 5", "stringLength", false, arg("value", "STRING"));
        builtin("string.contains", "包含判断", "String", "判断字符串是否包含子串", "BOOLEAN",
            "string.contains('hello','ll') => true", "stringContains", false,
            arg("value", "STRING"), arg("keyword", "STRING"));
        builtin("string.startsWith", "前缀判断", "String", "判断字符串前缀", "BOOLEAN",
            "string.startsWith('hello','he') => true", "stringStartsWith", false,
            arg("value", "STRING"), arg("prefix", "STRING"));
        builtin("string.endsWith", "后缀判断", "String", "判断字符串后缀", "BOOLEAN",
            "string.endsWith('hello','lo') => true", "stringEndsWith", false,
            arg("value", "STRING"), arg("suffix", "STRING"));
        builtin("string.indexOf", "子串位置", "String", "返回子串首次出现位置", "NUMBER",
            "string.indexOf('hello','l') => 2", "stringIndexOf", false,
            arg("value", "STRING"), arg("keyword", "STRING"));
        builtin("string.replace", "字符串替换", "String", "替换字符串片段", "STRING",
            "string.replace('hello','l','x') => 'hexxo'", "stringReplace", false,
            arg("value", "STRING"), arg("target", "STRING"), arg("replacement", "STRING"));
        builtin("seq.list", "创建列表", "Collection", "按入参顺序创建列表", "COLLECTION",
            "seq.list(1,2,3) => [1,2,3]", "seqList", true);
        builtin("seq.set", "创建集合", "Collection", "按入参顺序创建去重集合", "COLLECTION",
            "seq.set(1,2,3) => [1,2,3]", "seqSet", true);
        builtin("seq.map", "创建映射", "Collection", "按 key/value 入参创建映射", "MAP",
            "seq.map('a',1,'b',2) => {a:1,b:2}", "seqMap", true);
        builtin("date_to_string", "日期格式化", "Date", "将日期格式化为字符串", "STRING",
            "date_to_string(date, 'yyyy-MM-dd')", "dateToString", false,
            arg("date", "DATE"), arg("pattern", "STRING"));
    }

    private void builtin(String code,
                         String displayName,
                         String category,
                         String description,
                         String returnType,
                         String example,
                         String methodName,
                         boolean variadic,
                         FormulaFunctionDefinition.ArgumentDefinition... arguments) {
        FormulaFunctionDefinition.Builder builder = FormulaFunctionDefinition.builder()
            .functionCode(code)
            .displayName(displayName)
            .category(category)
            .description(description)
            .sourceType("BUILTIN")
            .returnType(returnType)
            .example(example)
            .implementationType(FormulaFunctionDefinition.IMPLEMENTATION_JAVA_BEAN)
            .beanName("formulaBuiltinFunctionProvider")
            .methodName(methodName)
            .timeoutMs(1000L)
            .variadic(variadic);
        Arrays.stream(arguments).forEach(argument ->
            builder.argument(argument.name(), argument.type(), argument.required()));
        registerDefinition(builder.build());
    }

    private FormulaFunctionDefinition.ArgumentDefinition arg(String name, String type) {
        return new FormulaFunctionDefinition.ArgumentDefinition(name, type, true);
    }

    private static class RegisteredAviatorFunction extends AbstractFunction {
        private final String functionCode;
        private final FormulaFunctionRegistry registry;

        private RegisteredAviatorFunction(String functionCode, FormulaFunctionRegistry registry) {
            this.functionCode = functionCode;
            this.registry = registry;
        }

        @Override
        public String getName() {
            return functionCode;
        }

        @Override
        public AviatorObject call(Map<String, Object> env) {
            return invoke(env);
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
            return invoke(env, arg1);
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            return invoke(env, arg1, arg2);
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1,
                                  AviatorObject arg2,
                                  AviatorObject arg3) {
            return invoke(env, arg1, arg2, arg3);
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1,
                                  AviatorObject arg2,
                                  AviatorObject arg3,
                                  AviatorObject arg4) {
            return invoke(env, arg1, arg2, arg3, arg4);
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1,
                                  AviatorObject arg2,
                                  AviatorObject arg3,
                                  AviatorObject arg4,
                                  AviatorObject arg5) {
            return invoke(env, arg1, arg2, arg3, arg4, arg5);
        }

        private AviatorObject invoke(Map<String, Object> env, AviatorObject... args) {
            Object[] values = Arrays.stream(args)
                .map(arg -> arg.getValue(env))
                .toArray(Object[]::new);
            Object result = registry.invoke(functionCode, values);
            return result == null ? AviatorNil.NIL : AviatorRuntimeJavaType.valueOf(FormulaSandboxValues.copy(result));
        }
    }
}
