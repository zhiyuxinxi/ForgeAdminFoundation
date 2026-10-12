package com.mdframe.forge.plugin.generator.service.formula;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Invokes registered formula functions through approved Java Bean methods.
 */
@Component
public class FormulaFunctionInvoker {

    private final Function<String, Object> beanResolver;
    private final FormulaRuntimeProperties runtimeProperties;

    @Autowired
    public FormulaFunctionInvoker(ApplicationContext applicationContext,
                                  FormulaRuntimeProperties runtimeProperties) {
        this(applicationContext::getBean, runtimeProperties);
    }

    public FormulaFunctionInvoker(ApplicationContext applicationContext) {
        this(applicationContext::getBean, new FormulaRuntimeProperties());
    }

    public FormulaFunctionInvoker(Map<String, Object> beans) {
        this(beans::get, new FormulaRuntimeProperties());
    }

    public FormulaFunctionInvoker(Map<String, Object> beans,
                                  FormulaRuntimeProperties runtimeProperties) {
        this(beans::get, runtimeProperties);
    }

    public FormulaFunctionInvoker(Function<String, Object> beanResolver) {
        this(beanResolver, new FormulaRuntimeProperties());
    }

    public FormulaFunctionInvoker(Function<String, Object> beanResolver,
                                  FormulaRuntimeProperties runtimeProperties) {
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver must not be null");
        this.runtimeProperties = runtimeProperties == null ? new FormulaRuntimeProperties() : runtimeProperties;
    }

    public Object invoke(FormulaFunctionDefinition definition, Object[] args) {
        Objects.requireNonNull(definition, "definition must not be null");
        Object[] actualArgs = args == null ? new Object[0] : args;
        validateEnabled(definition);
        validateArgumentSchema(definition, actualArgs);

        Object bean = beanResolver.apply(definition.getBeanName());
        if (bean == null) {
            throw new IllegalArgumentException("Formula function bean not found: " + definition.getBeanName());
        }

        Method method = findMethod(bean, definition, actualArgs.length);
        Object[] invokeArgs = adaptArguments(method, actualArgs);

        long start = System.currentTimeMillis();
        try {
            Object result = method.invoke(bean, invokeArgs);
            long elapsedMs = System.currentTimeMillis() - start;
            long timeoutMs = runtimeProperties.effectiveFunctionTimeoutMs(definition.getTimeoutMs());
            if (elapsedMs > timeoutMs) {
                throw new IllegalStateException("Formula function timed out: "
                    + definition.getFunctionCode() + ", timeoutMs=" + timeoutMs + ", elapsedMs=" + elapsedMs);
            }
            validateReturnType(definition, result);
            return result;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access formula function: "
                + definition.getFunctionCode(), e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Formula function invocation failed: "
                + definition.getFunctionCode() + ", " + target.getMessage(), target);
        }
    }

    public void validateArgumentSchema(FormulaFunctionDefinition definition, Object[] args) {
        int requiredCount = definition.requiredArgumentCount();
        if (args.length < requiredCount) {
            throw new IllegalArgumentException("Function " + definition.getFunctionCode()
                + " requires at least " + requiredCount + " arguments");
        }
        if (!definition.isVariadic() && args.length > definition.getArguments().size()) {
            throw new IllegalArgumentException("Function " + definition.getFunctionCode()
                + " accepts " + definition.getArguments().size() + " arguments");
        }

        int schemaCount = Math.min(args.length, definition.getArguments().size());
        for (int i = 0; i < schemaCount; i++) {
            FormulaFunctionDefinition.ArgumentDefinition argument = definition.getArguments().get(i);
            Object value = args[i];
            if (value == null) {
                if (argument.required()) {
                    throw new IllegalArgumentException("Function " + definition.getFunctionCode()
                        + " argument [" + argument.name() + "] must not be null");
                }
                continue;
            }
            if (!matchesType(argument.type(), value)) {
                throw new IllegalArgumentException("Function " + definition.getFunctionCode()
                    + " argument [" + argument.name() + "] expects " + argument.type());
            }
        }
    }

    private void validateEnabled(FormulaFunctionDefinition definition) {
        if (!definition.isEnabled()) {
            throw new IllegalStateException("Formula function is disabled: " + definition.getFunctionCode());
        }
        if (!definition.isJavaBean()) {
            throw new IllegalStateException("Unsupported formula function implementation: "
                + definition.getImplementationType());
        }
    }

    private Method findMethod(Object bean, FormulaFunctionDefinition definition, int argCount) {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(definition.getMethodName())) {
                continue;
            }
            if (method.isVarArgs() && argCount >= method.getParameterCount() - 1) {
                return method;
            }
            if (!method.isVarArgs() && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new IllegalArgumentException("Formula function method not found: "
            + definition.getBeanName() + "." + definition.getMethodName()
            + ", argCount=" + argCount);
    }

    private Object[] adaptArguments(Method method, Object[] args) {
        if (!method.isVarArgs()) {
            return convertArguments(method.getParameterTypes(), args);
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] invokeArgs = new Object[parameterTypes.length];
        int fixedCount = parameterTypes.length - 1;
        for (int i = 0; i < fixedCount; i++) {
            invokeArgs[i] = convertArgument(parameterTypes[i], args[i]);
        }

        Class<?> varArgType = parameterTypes[parameterTypes.length - 1].getComponentType();
        int varArgCount = args.length - fixedCount;
        Object varArgs = Array.newInstance(varArgType, varArgCount);
        for (int i = 0; i < varArgCount; i++) {
            Array.set(varArgs, i, convertArgument(varArgType, args[fixedCount + i]));
        }
        invokeArgs[parameterTypes.length - 1] = varArgs;
        return invokeArgs;
    }

    private Object[] convertArguments(Class<?>[] parameterTypes, Object[] args) {
        Object[] converted = Arrays.copyOf(args, args.length);
        for (int i = 0; i < converted.length; i++) {
            converted[i] = convertArgument(parameterTypes[i], args[i]);
        }
        return converted;
    }

    private Object convertArgument(Class<?> targetType, Object value) {
        if (value == null || targetType == Object.class || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if ((targetType == Integer.class || targetType == int.class) && value instanceof Number number) {
            return number.intValue();
        }
        if ((targetType == Long.class || targetType == long.class) && value instanceof Number number) {
            return number.longValue();
        }
        if ((targetType == Double.class || targetType == double.class) && value instanceof Number number) {
            return number.doubleValue();
        }
        if ((targetType == Float.class || targetType == float.class) && value instanceof Number number) {
            return number.floatValue();
        }
        if ((targetType == Boolean.class || targetType == boolean.class) && value instanceof Boolean bool) {
            return bool;
        }
        return value;
    }

    private void validateReturnType(FormulaFunctionDefinition definition, Object result) {
        if (result == null || "ANY".equalsIgnoreCase(definition.getReturnType())) {
            return;
        }
        if (!matchesType(definition.getReturnType(), result)) {
            throw new IllegalStateException("Function " + definition.getFunctionCode()
                + " return type expects " + definition.getReturnType());
        }
    }

    private boolean matchesType(String type, Object value) {
        if (type == null || type.isBlank() || "ANY".equalsIgnoreCase(type)) {
            return true;
        }
        return switch (type.toUpperCase()) {
            case "NUMBER" -> value instanceof Number;
            case "STRING" -> value instanceof CharSequence;
            case "BOOLEAN" -> value instanceof Boolean;
            case "DATE" -> value instanceof java.util.Date
                || value instanceof java.time.temporal.TemporalAccessor
                || value instanceof Number
                || value instanceof CharSequence;
            case "COLLECTION" -> value instanceof java.util.Collection<?>;
            case "MAP" -> value instanceof Map<?, ?>;
            default -> true;
        };
    }
}
