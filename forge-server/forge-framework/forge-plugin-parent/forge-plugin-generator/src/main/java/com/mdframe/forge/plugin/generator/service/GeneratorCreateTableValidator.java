package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.NamedConstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** The legacy generator accepts SQL, but executes only this reconstructed subset. */
public final class GeneratorCreateTableValidator {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");
    private static final Pattern STRING = Pattern.compile("'(?:[^'\\\\]|'')*'", Pattern.DOTALL);
    private static final Set<String> TYPES = Set.of("BIGINT", "INT", "INTEGER", "SMALLINT", "TINYINT",
            "DECIMAL", "NUMERIC", "VARCHAR", "CHAR", "TEXT", "LONGTEXT", "DATE", "DATETIME", "TIMESTAMP", "TIME", "BOOLEAN", "JSON");
    private static final Set<String> REQUIRED = Set.of("id", "tenant_id", "create_by", "create_time",
            "create_dept", "update_by", "update_time");

    private GeneratorCreateTableValidator() { }

    public static ValidatedCreateTable validate(String sql) {
        if (sql == null || sql.isBlank() || sql.length() > 65536) throw invalid();
        try {
            var statements = CCJSqlParserUtil.parseStatements(sql, parser -> parser.withTimeOut(1000));
            if (statements.size() != 1 || !(statements.get(0) instanceof CreateTable table)) throw invalid();
            if (table.getSelect() != null || table.getLikeTable() != null || table.isOrReplace()
                    || table.isUnlogged() || table.getRowMovement() != null || table.getSpannerInterleaveIn() != null
                    || !empty(table.getCreateOptionsStrings()) || !empty(table.getColumns())
                    || table.getTable().getSchemaName() != null
                    || !table.getTable().getFullyQualifiedName().equals(table.getTable().getName())) throw invalid();
            String name = identifier(table.getTable().getName());
            if (!name.startsWith("biz_")) throw invalid();
            List<ColumnDefinition> columns = table.getColumnDefinitions();
            if (columns == null || columns.isEmpty() || columns.size() > 128) throw invalid();
            Set<String> names = new HashSet<>();
            List<String> fragments = new ArrayList<>();
            boolean primaryKey = false;
            for (ColumnDefinition column : columns) {
                String columnName = identifier(column.getColumnName());
                if (!names.add(columnName.toLowerCase(Locale.ROOT))) throw invalid();
                String type = column.getColDataType().getDataType().toUpperCase(Locale.ROOT);
                if (!TYPES.contains(type) || !empty(column.getColDataType().getArrayData())) throw invalid();
                List<String> arguments = column.getColDataType().getArgumentsStringList();
                if (!empty(arguments)) {
                    if (arguments.size() > 2 || arguments.stream().anyMatch(value -> !value.matches("[0-9]{1,4}"))) throw invalid();
                    type += "(" + String.join(",", arguments) + ")";
                }
                if (Set.of("id", "tenant_id", "create_by", "create_dept", "update_by").contains(columnName)
                        && !type.equals("BIGINT")) throw invalid();
                if (Set.of("create_time", "update_time").contains(columnName)
                        && !(type.equals("DATETIME") || type.equals("TIMESTAMP"))) throw invalid();
                String specs = columnSpecs(column.getColumnSpecs());
                if (specs.contains("PRIMARY KEY")) {
                    if (!columnName.equals("id") || primaryKey) throw invalid();
                    primaryKey = true;
                }
                fragments.add("`" + columnName + "` " + type + specs);
            }
            if (!names.containsAll(REQUIRED)) throw invalid();
            if (table.getIndexes() != null) {
                for (Index index : table.getIndexes()) {
                    if ((index.getClass() != Index.class && index.getClass() != NamedConstraint.class)
                            || !empty(index.getIndexSpec()) || index.getUsing() != null
                            || index.getCommentText() != null) throw invalid();
                    String type = index.getType().toUpperCase(Locale.ROOT);
                    if (!Set.of("PRIMARY KEY", "UNIQUE", "UNIQUE KEY", "UNIQUE INDEX", "KEY", "INDEX").contains(type)) throw invalid();
                    List<String> indexColumns = index.getColumnsNames();
                    if (empty(indexColumns) || indexColumns.size() > 8) throw invalid();
                    List<String> quoted = new ArrayList<>();
                    for (String raw : indexColumns) {
                        String columnName = identifier(raw);
                        if (!names.contains(columnName.toLowerCase(Locale.ROOT))) throw invalid();
                        quoted.add("`" + columnName + "`");
                    }
                    // No prefix lengths, index expressions, referential constraints or extensions.
                    if (index.getColumns().stream().anyMatch(column -> !empty(column.getParams()))) throw invalid();
                    if (type.equals("PRIMARY KEY")) {
                        if (primaryKey || !quoted.equals(List.of("`id`"))) throw invalid();
                        primaryKey = true;
                    }
                    String indexName = type.equals("PRIMARY KEY") ? "" : " `" + identifier(index.getName()) + "`";
                    fragments.add(type + indexName + " (" + String.join(", ", quoted) + ")");
                }
            }
            if (!primaryKey) throw invalid();
            String options = tableOptions(table.getTableOptionsStrings());
            return new ValidatedCreateTable(name, "CREATE TABLE " + (table.isIfNotExists() ? "IF NOT EXISTS " : "")
                    + "`" + name + "` (" + String.join(", ", fragments) + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" + options);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private static String columnSpecs(List<String> tokens) {
        if (empty(tokens)) return "";
        List<String> accepted = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toUpperCase(Locale.ROOT);
            switch (token) {
                case "NULL", "AUTO_INCREMENT", "UNSIGNED", "UNIQUE" -> accepted.add(token);
                case "NOT" -> {
                    if (++i >= tokens.size() || !tokens.get(i).equalsIgnoreCase("NULL")) throw invalid();
                    accepted.add("NOT NULL");
                }
                case "PRIMARY" -> {
                    if (++i >= tokens.size() || !tokens.get(i).equalsIgnoreCase("KEY")) throw invalid();
                    accepted.add("PRIMARY KEY");
                }
                case "DEFAULT" -> {
                    if (++i >= tokens.size()) throw invalid();
                    String value = tokens.get(i);
                    if (!(STRING.matcher(value).matches() || value.matches("-?[0-9]+(?:\\.[0-9]+)?")
                            || value.equalsIgnoreCase("NULL") || value.equalsIgnoreCase("CURRENT_TIMESTAMP"))) throw invalid();
                    accepted.add("DEFAULT " + value);
                }
                case "COMMENT" -> {
                    if (++i >= tokens.size() || !STRING.matcher(tokens.get(i)).matches()) throw invalid();
                    accepted.add("COMMENT " + tokens.get(i));
                }
                default -> throw invalid();
            }
        }
        return " " + String.join(" ", accepted);
    }

    private static String tableOptions(List<String> tokens) {
        if (empty(tokens)) return "";
        String comment = "";
        for (int i = 0; i < tokens.size(); i++) {
            String option = tokens.get(i).toUpperCase(Locale.ROOT);
            if (option.equals("DEFAULT")) {
                if (++i >= tokens.size()) throw invalid();
                option = tokens.get(i).toUpperCase(Locale.ROOT);
            }
            if (option.equals("CHARACTER")) {
                if (++i >= tokens.size() || !tokens.get(i).equalsIgnoreCase("SET")) throw invalid();
                option = "CHARSET";
            }
            if (++i >= tokens.size()) throw invalid();
            if (tokens.get(i).equals("=") && ++i >= tokens.size()) throw invalid();
            String value = tokens.get(i);
            switch (option) {
                case "ENGINE" -> { if (!value.equalsIgnoreCase("InnoDB")) throw invalid(); }
                case "CHARSET" -> { if (!value.equalsIgnoreCase("utf8mb4")) throw invalid(); }
                case "COLLATE" -> {
                    if (!Set.of("utf8mb4_unicode_ci", "utf8mb4_general_ci", "utf8mb4_0900_ai_ci", "utf8mb4_bin").contains(value.toLowerCase(Locale.ROOT))) throw invalid();
                    comment += " COLLATE=" + value;
                }
                case "COMMENT" -> {
                    if (!STRING.matcher(value).matches()) throw invalid();
                    comment += " COMMENT=" + value;
                }
                default -> throw invalid();
            }
        }
        return comment;
    }

    private static String identifier(String value) {
        if (value != null && value.startsWith("`") && value.endsWith("`")) value = value.substring(1, value.length() - 1);
        if (value == null || !IDENTIFIER.matcher(value).matches()) throw invalid();
        return value;
    }

    private static boolean empty(List<?> values) { return values == null || values.isEmpty(); }

    private static BusinessException invalid() {
        return new BusinessException(UiMessages.label(
                "仅允许包含主键、租户及审计字段的 biz_ 单表建表语句；不支持查询、复制表、跨库或扩展DDL",
                "Only one biz_ table with a primary key, tenant and audit columns is allowed; queries, copied tables, cross-schema and extended DDL are not supported"));
    }

    public record ValidatedCreateTable(String tableName, String sql) { }
}
