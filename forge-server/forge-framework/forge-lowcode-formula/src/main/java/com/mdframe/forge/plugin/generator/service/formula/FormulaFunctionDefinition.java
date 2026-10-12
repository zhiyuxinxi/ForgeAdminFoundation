package com.mdframe.forge.plugin.generator.service.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Runtime definition of one formula function.
 */
public class FormulaFunctionDefinition {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String IMPLEMENTATION_JAVA_BEAN = "JAVA_BEAN";

    private final String functionCode;
    private final String displayName;
    private final String category;
    private final String description;
    private final String sourceType;
    private final String returnType;
    private final String example;
    private final String status;
    private final String implementationType;
    private final String beanName;
    private final String methodName;
    private final long timeoutMs;
    private final boolean variadic;
    private final List<ArgumentDefinition> arguments;

    private FormulaFunctionDefinition(Builder builder) {
        this.functionCode = requireText(builder.functionCode, "functionCode");
        this.displayName = defaultText(builder.displayName, this.functionCode);
        this.category = defaultText(builder.category, "Other");
        this.description = defaultText(builder.description, "");
        this.sourceType = defaultText(builder.sourceType, "BUILTIN");
        this.returnType = defaultText(builder.returnType, "ANY");
        this.example = defaultText(builder.example, "");
        this.status = defaultText(builder.status, STATUS_ENABLED);
        this.implementationType = defaultText(builder.implementationType, IMPLEMENTATION_JAVA_BEAN);
        this.beanName = builder.beanName;
        this.methodName = builder.methodName;
        this.timeoutMs = builder.timeoutMs > 0 ? builder.timeoutMs : 1000L;
        this.variadic = builder.variadic;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));

        if (isJavaBean()) {
            requireText(beanName, "beanName");
            requireText(methodName, "methodName");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getExample() {
        return example;
    }

    public String getStatus() {
        return status;
    }

    public String getImplementationType() {
        return implementationType;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isVariadic() {
        return variadic;
    }

    public List<ArgumentDefinition> getArguments() {
        return arguments;
    }

    public boolean isEnabled() {
        return STATUS_ENABLED.equalsIgnoreCase(status);
    }

    public boolean isJavaBean() {
        return IMPLEMENTATION_JAVA_BEAN.equalsIgnoreCase(implementationType);
    }

    public int requiredArgumentCount() {
        int count = 0;
        for (ArgumentDefinition argument : arguments) {
            if (argument.required()) {
                count++;
            }
        }
        return count;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static class Builder {
        private String functionCode;
        private String displayName;
        private String category;
        private String description;
        private String sourceType;
        private String returnType;
        private String example;
        private String status;
        private String implementationType;
        private String beanName;
        private String methodName;
        private long timeoutMs;
        private boolean variadic;
        private final List<ArgumentDefinition> arguments = new ArrayList<>();

        public Builder functionCode(String functionCode) {
            this.functionCode = functionCode;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder example(String example) {
            this.example = example;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder implementationType(String implementationType) {
            this.implementationType = implementationType;
            return this;
        }

        public Builder beanName(String beanName) {
            this.beanName = beanName;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder variadic(boolean variadic) {
            this.variadic = variadic;
            return this;
        }

        public Builder argument(String name, String type, boolean required) {
            this.arguments.add(new ArgumentDefinition(name, type, required));
            return this;
        }

        public FormulaFunctionDefinition build() {
            return new FormulaFunctionDefinition(this);
        }
    }

    public record ArgumentDefinition(String name, String type, boolean required) {
        public ArgumentDefinition {
            Objects.requireNonNull(name, "name must not be null");
            type = type == null || type.isBlank() ? "ANY" : type;
        }
    }
}
