package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;

import java.util.Set;
import java.util.regex.Pattern;

/** Request and response projection rules used by the downloadable Java templates. */
public final class CodegenFieldContract {
    private static final Set<String> SERVER_FIELDS = Set.of(
            "tenantId", "createBy", "createDept", "createTime", "updateBy", "updateTime", "delFlag");
    private static final Pattern CHARACTER_LENGTH = Pattern.compile("(?i)^(?:var)?char\\((\\d+)\\).*$");

    public boolean writable(GenTableColumn column) {
        return !SERVER_FIELDS.contains(column.getJavaField())
                && (Integer.valueOf(1).equals(column.getIsInsert()) || Integer.valueOf(1).equals(column.getIsEdit()));
    }

    public boolean readable(GenTableColumn column) {
        return !Set.of("tenantId", "delFlag").contains(column.getJavaField());
    }

    public int maxLength(GenTableColumn column) {
        if (!"String".equals(column.getJavaType()) || column.getColumnType() == null) {
            return 0;
        }
        var match = CHARACTER_LENGTH.matcher(column.getColumnType().trim());
        if (!match.matches()) {
            return 0;
        }
        try {
            return Integer.parseInt(match.group(1));
        } catch (NumberFormatException invalidMetadata) {
            return 0;
        }
    }
}
