package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.List;
import java.util.Map;

/**
 * Data provider interface for aggregate formulas.
 * <p>
 * Implementations provide detail records for a given relation.
 * <b>Phase 4</b> will provide a DB-backed implementation that queries
 * child tables via {@code AiBusinessObjectRelation} and dynamic CRUD.
 * <p>
 * This interface is deliberately minimal — only the data-fetch concern
 * is exposed. Filtering and computation are handled by {@link AggregateEngine}.
 */
@FunctionalInterface
public interface AggregateDataProvider {

    /**
     * Fetch detail records for a business object relation.
     *
     * @param relationCode the relation code from {@link AggregateConfig#getRelationCode()}
     * @param context      the current row context (main record values, for scoping)
     * @return list of detail records as field→value maps
     */
    List<Map<String, Object>> getDetailRecords(String relationCode, Map<String, Object> context);
}
