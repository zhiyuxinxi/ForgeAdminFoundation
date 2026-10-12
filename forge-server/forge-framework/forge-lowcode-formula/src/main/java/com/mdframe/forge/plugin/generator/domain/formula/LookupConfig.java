package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * LOOKUP formula configuration.
 * <p>
 * Describes how a field reads a value from a related target object. It only
 * stores object/relation metadata and never stores table names, SQL, or mapper
 * method names.
 */
public class LookupConfig {
    /** Object relation code */
    private final String relationCode;
    /** Target business object code */
    private final String targetObjectCode;
    /** Current object field used as lookup key */
    private final String sourceField;
    /** Target object field used to match sourceField */
    private final String targetField;
    /** Target object field returned as formula value */
    private final String returnField;
    /** Optional fallback value when no target row is matched */
    private final Object notFoundValue;

    @JsonCreator
    public LookupConfig(@JsonProperty("relationCode") String relationCode,
                        @JsonProperty("targetObjectCode") String targetObjectCode,
                        @JsonProperty("sourceField") String sourceField,
                        @JsonProperty("targetField") String targetField,
                        @JsonProperty("returnField") String returnField,
                        @JsonProperty("notFoundValue") Object notFoundValue) {
        this.relationCode = requireText(relationCode, "relationCode");
        this.targetObjectCode = requireText(targetObjectCode, "targetObjectCode");
        this.sourceField = requireText(sourceField, "sourceField");
        this.targetField = requireText(targetField, "targetField");
        this.returnField = requireText(returnField, "returnField");
        this.notFoundValue = notFoundValue;
    }

    public String getRelationCode() { return relationCode; }
    public String getTargetObjectCode() { return targetObjectCode; }
    public String getSourceField() { return sourceField; }
    public String getTargetField() { return targetField; }
    public String getReturnField() { return returnField; }
    public Object getNotFoundValue() { return notFoundValue; }

    public boolean hasNotFoundValue() { return notFoundValue != null; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LookupConfig that)) return false;
        return relationCode.equals(that.relationCode)
            && targetObjectCode.equals(that.targetObjectCode)
            && sourceField.equals(that.sourceField)
            && targetField.equals(that.targetField)
            && returnField.equals(that.returnField)
            && Objects.equals(notFoundValue, that.notFoundValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationCode, targetObjectCode, sourceField, targetField, returnField, notFoundValue);
    }

    @Override
    public String toString() {
        return "LOOKUP(" + relationCode + "." + returnField + ")";
    }
}
