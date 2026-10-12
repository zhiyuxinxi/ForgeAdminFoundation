package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import java.util.Map;

/** Display-only translations and SQL literals shared by the native generation templates. */
public final class CodegenI18n {
    public String english(String source, String translated) {
        return UiMessages.englishLabel(source, translated);
    }
    public String column(GenTableColumn column) {
        return english(column.getColumnComment(), column.getColumnCommentEn());
    }
    public String dictionary(GenTableColumn column, String value, String label) {
        Map<String, Map<String, String>> labels = column.getDictLabelsI18n();
        Map<String, String> translations = labels == null ? null : labels.get(value);
        return english(label, translations == null ? null : translations.get("en-US"));
    }
    public String javaLiteral(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
    public String sql(String value) {
        return value == null ? "NULL" : "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }
}
