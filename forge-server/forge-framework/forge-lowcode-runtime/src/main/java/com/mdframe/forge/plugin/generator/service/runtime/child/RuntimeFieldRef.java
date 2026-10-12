package com.mdframe.forge.plugin.generator.service.runtime.child;


/** Resolved runtime FieldRef metadata. */
public record RuntimeFieldRef(String fieldName,
                               String modelCode,
                               String sourceField,
                               String columnName,
                               String tableName,
                               String tableAlias,
                               boolean primary) {
}
