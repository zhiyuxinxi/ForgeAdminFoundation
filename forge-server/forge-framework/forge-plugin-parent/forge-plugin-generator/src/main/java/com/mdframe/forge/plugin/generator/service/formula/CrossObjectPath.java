package com.mdframe.forge.plugin.generator.service.formula;

import java.util.Objects;

/**
 * Parsed one-hop cross-object path.
 */
public class CrossObjectPath {

    private final String relationAlias;
    private final String fieldCode;

    private CrossObjectPath(String relationAlias, String fieldCode) {
        this.relationAlias = relationAlias;
        this.fieldCode = fieldCode;
    }

    public static CrossObjectPath parse(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("cross-object path must not be blank");
        }
        String trimmed = path.trim();
        int firstDot = trimmed.indexOf('.');
        if (firstDot <= 0 || firstDot == trimmed.length() - 1 || firstDot != trimmed.lastIndexOf('.')) {
            throw new IllegalArgumentException("cross-object path must be one-hop relation.field");
        }
        return new CrossObjectPath(trimmed.substring(0, firstDot), trimmed.substring(firstDot + 1));
    }

    public String getRelationAlias() { return relationAlias; }

    public String getFieldCode() { return fieldCode; }

    public String asExpressionPath() {
        return relationAlias + "." + fieldCode;
    }

    @Override
    public String toString() {
        return asExpressionPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrossObjectPath that)) return false;
        return relationAlias.equals(that.relationAlias) && fieldCode.equals(that.fieldCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationAlias, fieldCode);
    }
}
