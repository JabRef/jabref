package org.jabref.model.search;

import java.util.Set;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public enum PostgreConstants {
    ENTRY_ID("entry_id"),
    FIELD_NAME("field_name"),
    FIELD_VALUE_LITERAL("field_value_literal"), // contains the value as-is
    FIELD_VALUE_TRANSFORMED("field_value_transformed"); // contains the value transformed for better querying

    public static final Set<Field> MULTI_VALUE_FIELDS = Set.of(
            StandardField.AUTHOR,
            StandardField.EDITOR,
            StandardField.CROSSREF,
            StandardField.KEYWORDS,
            StandardField.GROUPS,
            StandardField.FILE);
    private final String columnName;

    PostgreConstants(String columnName) {
        this.columnName = columnName;
    }

    public String getIndexName(String tableName) {
        return "%s_%s_index".formatted(tableName, this.columnName);
    }

    @Override
    public String toString() {
        return columnName;
    }
}
