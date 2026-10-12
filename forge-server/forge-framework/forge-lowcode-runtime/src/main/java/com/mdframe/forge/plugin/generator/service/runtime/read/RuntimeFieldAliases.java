package com.mdframe.forge.plugin.generator.service.runtime.read;

import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

/** Supplements declared field names without overriding explicit query results. */
@RequiredArgsConstructor
public final class RuntimeFieldAliases {
    private final RuntimeFieldMetadata metadata;

    public void aliases(List<Map<String, Object>> rows, LowcodeRuntimeConfig config) {
        if (rows == null || rows.isEmpty() || config == null) {
            return;
        }
        LowcodeModelSchema modelSchema = metadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            aliasRow(row, modelSchema.getFields());
        }
    }

    private void aliasRow(Map<String, Object> row, List<LowcodeFieldSchema> fields) {
        if (row == null) return;
        for (LowcodeFieldSchema field : fields) {
            String name = field == null ? null : StringUtils.trimToNull(field.getField());
            String column = field == null ? null : StringUtils.trimToNull(field.getColumnName());
            if (name == null || column == null || row.containsKey(name)) continue;
            if (row.containsKey(column)) {
                row.put(name, row.get(column));
            } else {
                String camel = DynamicQueryGenerator.snakeToCamel(column);
                if (row.containsKey(camel)) row.put(name, row.get(camel));
            }
        }
    }
}
