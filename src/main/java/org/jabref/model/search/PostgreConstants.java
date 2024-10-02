package org.jabref.model.search;

public enum PostgreConstants {
    BIB_FIELDS_SCHEME("bib_fields"),
    SPLIT_TABLE_SUFFIX("_split_values"),
    ENTRY_ID("entry_id"),
    FIELD_NAME("field_name"),
    FIELD_VALUE_LITERAL("field_value_literal"), // contains the value as-is
    FIELD_VALUE_TRANSFORMED("field_value_transformed"), // contains the value transformed for better querying
    LINKED_FILES_SCHEME("linked_files"),
    CONTENT_TABLE_SUFFIX("_file_content"),
    FILE_PATH("path"),
    FILE_MODIFIED("modified"),
    LINKED_ENTRIES("entries"),
    PAGE_NUMBER("pageNumber"),
    PAGE_CONTENT("content"),
    PAGE_ANNOTATIONS("annotations");

    private final String value;

    PostgreConstants(String value) {
        this.value = value;
    }

    public static String getMainTableSchemaReference(String mainTable) {
        return BIB_FIELDS_SCHEME + ".\"" + mainTable + "\"";
    }

    public static String getSplitTableSchemaReference(String mainTable) {
        return BIB_FIELDS_SCHEME + ".\"" + mainTable + SPLIT_TABLE_SUFFIX + "\"";
    }

    @Override
    public String toString() {
        return value;
    }
}
