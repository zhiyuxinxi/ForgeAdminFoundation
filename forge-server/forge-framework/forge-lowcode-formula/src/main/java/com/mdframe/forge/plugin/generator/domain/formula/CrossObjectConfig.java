package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Cross-object formula path configuration.
 * <p>
 * The first release only allows one-hop paths such as {@code customer.level}.
 */
public class CrossObjectConfig {
    /** One-hop path expression: relationAlias.fieldCode */
    private final String path;
    /** Object relation code used to resolve the path */
    private final String relationCode;
    /** Target business object code */
    private final String targetObjectCode;
    /** Target object field returned by this path */
    private final String returnField;
    /** STORED formula recompute strategy */
    private final CrossObjectRecomputeMode recomputeMode;

    @JsonCreator
    public CrossObjectConfig(@JsonProperty("path") String path,
                             @JsonProperty("relationCode") String relationCode,
                             @JsonProperty("targetObjectCode") String targetObjectCode,
                             @JsonProperty("returnField") String returnField,
                             @JsonProperty("recomputeMode") CrossObjectRecomputeMode recomputeMode) {
        this.path = requireText(path, "path");
        validateOneHopPath(this.path);
        this.relationCode = requireText(relationCode, "relationCode");
        this.targetObjectCode = requireText(targetObjectCode, "targetObjectCode");
        this.returnField = requireText(returnField, "returnField");
        this.recomputeMode = recomputeMode == null ? CrossObjectRecomputeMode.ASYNC : recomputeMode;
        validateReturnField();
    }

    public String getPath() { return path; }
    public String getRelationCode() { return relationCode; }
    public String getTargetObjectCode() { return targetObjectCode; }
    public String getReturnField() { return returnField; }
    public CrossObjectRecomputeMode getRecomputeMode() { return recomputeMode; }

    public String getRelationAlias() {
        return path.substring(0, path.indexOf('.'));
    }

    public String getPathField() {
        return path.substring(path.indexOf('.') + 1);
    }

    private void validateReturnField() {
        if (!returnField.equals(getPathField())) {
            throw new IllegalArgumentException("returnField must match path target field");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static void validateOneHopPath(String path) {
        int firstDot = path.indexOf('.');
        if (firstDot <= 0 || firstDot == path.length() - 1 || firstDot != path.lastIndexOf('.')) {
            throw new IllegalArgumentException("cross-object path must be one-hop relation.field");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrossObjectConfig that)) return false;
        return path.equals(that.path)
            && relationCode.equals(that.relationCode)
            && targetObjectCode.equals(that.targetObjectCode)
            && returnField.equals(that.returnField)
            && recomputeMode == that.recomputeMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, relationCode, targetObjectCode, returnField, recomputeMode);
    }

    @Override
    public String toString() {
        return path + "@" + relationCode + "[" + recomputeMode + "]";
    }
}
