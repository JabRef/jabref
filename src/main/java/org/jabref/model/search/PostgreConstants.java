package org.jabref.model.search;

public enum PostgreConstants {
    BIB_FIELDS_SCHEME("bib_fields"),
    LINKED_FILES_SCHEME("linked_files"),
    ENTRY_ID("entry_id"),
    FIELD_NAME("field_name"),
    FIELD_VALUE_LITERAL("field_value_literal"), // contains the value as-is
    FIELD_VALUE_TRANSFORMED("field_value_transformed"), // contains the value transformed for better querying
    TABLE_NAME_SUFFIX("_split_values");
    private final String value;

    PostgreConstants(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
