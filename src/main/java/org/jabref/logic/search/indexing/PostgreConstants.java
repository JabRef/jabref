package org.jabref.logic.search.indexing;

public enum PostgreConstants {
    ENTRY_ID("entry_id"),
    FIELD_NAME("field_name"),
    FIELD_VALUE("field_value");

    private final String columnName;

    PostgreConstants(String columnName) {
        this.columnName = columnName;
    }

    public String getIndexName(String tableName) {
        return String.format("%s_%s_index", tableName, this.columnName);
    }

    @Override
    public String toString() {
        return columnName;
    }
}
