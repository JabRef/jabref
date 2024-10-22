package org.jabref.model.search;

import java.util.List;

public enum PostgreConstants {
    BIB_FIELDS_SCHEME("bib_fields"),
    SPLIT_TABLE_SUFFIX("_split_values"),
    ENTRY_ID("entry_id"),
    FIELD_NAME("field_name"),
    FIELD_VALUE_LITERAL("field_value_literal"), // contains the value as-is
    FIELD_VALUE_TRANSFORMED("field_value_transformed"); // contains the value transformed for better querying

    public static final List<String> POSTGRES_FUNCTIONS = List.of(
            // HTML highlighting function
            """
                CREATE OR REPLACE FUNCTION regexp_mark(string text, pattern text)
                    RETURNS text
                    LANGUAGE plpgsql
                AS
                $$
                BEGIN
                    RETURN regexp_replace(string, format('(%s)', pattern), '<mark style="background: orange">\\1</mark>', 'gi');
                END
                $$;
            """,

            // Extract positions of a pattern in a string
            // Source: https://www.postgresql.org/message-id/flat/0aabac3c-9049-4c55-a82d-a70c5ba43d4d%40www.fastmail.com
            """
                CREATE OR REPLACE FUNCTION regexp_positions(string text, pattern text, OUT start_pos integer, OUT end_pos integer)
                    RETURNS SETOF RECORD
                    LANGUAGE plpgsql
                AS
                $$
                DECLARE
                    match text;
                    remainder text := string;
                    len integer;
                    pos integer;
                BEGIN
                    end_pos := 0;
                    FOR match IN SELECT (regexp_matches(string, format('(%s)', pattern), 'gi'))[1] LOOP
                            len := length(match);
                            pos := position(match in remainder);
                            start_pos := pos + end_pos;
                            end_pos := start_pos + len - 1;
                            RETURN NEXT;
                            remainder := right(remainder, 1 - len - pos);
                        END LOOP;
                    RETURN;
                END
                $$;
            """
    );

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
