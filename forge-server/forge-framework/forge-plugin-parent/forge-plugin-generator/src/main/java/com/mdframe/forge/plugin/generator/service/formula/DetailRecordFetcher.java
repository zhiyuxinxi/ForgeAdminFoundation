package com.mdframe.forge.plugin.generator.service.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fetches detail records for aggregate formulas at runtime.
 * <p>
 * Phase 4A: test implementation returns empty list.
 * Phase 4B: real implementation queries the database via dynamic CRUD.
 */
@FunctionalInterface
public interface DetailRecordFetcher {

    /**
     * Fetch detail records from a child table.
     *
     * @param targetTableName  the child table name
     * @param joinField        the join field in the child table (maps to parent PK)
     * @param joinValue        the value to match in the join field
     * @param tenantId         tenant for multi-tenant isolation
     * @return list of detail records as field→value maps
     */
    List<Map<String, Object>> fetchDetailRecords(String targetTableName,
                                                   String joinField,
                                                   Object joinValue,
                                                   Long tenantId);

    /**
     * Batch fetch detail records by one join field.
     * <p>
     * Implementations backed by dynamic SQL should override this method with a
     * single IN query. The default keeps compatibility for existing tests.
     */
    default List<Map<String, Object>> fetchDetailRecordsBatch(String targetTableName,
                                                              String joinField,
                                                              Collection<?> joinValues,
                                                              Long tenantId) {
        if (joinValues == null || joinValues.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object joinValue : joinValues) {
            rows.addAll(fetchDetailRecords(targetTableName, joinField, joinValue, tenantId));
        }
        return rows;
    }
}
