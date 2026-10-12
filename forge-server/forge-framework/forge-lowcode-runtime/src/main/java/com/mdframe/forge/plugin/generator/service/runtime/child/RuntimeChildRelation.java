package com.mdframe.forge.plugin.generator.service.runtime.child;

import java.util.Map;

/** Resolved runtime ChildRelation metadata. */
public record RuntimeChildRelation(String modelCode,
                                    String tableName,
                                    String tableAlias,
                                    String childFkColumn,
                                    String mainColumn,
                                    String saveMode,
                                    Map<String, RuntimeFieldRef> fields,
                                    Map<String, RuntimeChildFieldRule> fieldRules) {
    public Object mainValue(
                                            Map<String, Object> primaryData,
                                            Object mainId,
                                            Map<String, Object> currentMainRecord) {
        if ("id".equals(mainColumn())) {
            return mainId;
        }
        if (primaryData.containsKey(mainColumn())) {
            return primaryData.get(mainColumn());
        }
        if (currentMainRecord != null) {
            return currentMainRecord.get(mainColumn());
        }
        return null;
    }
}
