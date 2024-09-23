package org.jabref.logic.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            // Default search, query without any field name (case insensitive contains)
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'compute'), compute",
            // case insensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~* 'compute'), title=compute",
            // case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~ 'compute'), title=!compute",
            // exact match case insensitive
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~* '\\ycompute\\y'), title==compute",
            // exact match case sensitive
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~ '\\ycompute\\y'), title==!compute",
            // negated case insensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~* 'compute'), title!=compute",
            // negated case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~ 'compute'), title !=! compute",
            // negated case insensitive exact match
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~* '\\ycompute\\y'), title !== compute",
            // negated case sensitive exact match
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~ '\\ycompute\\y'), title !==! compute",
    })

    void conversion(String expected, String input) {
        assertEquals(expected, SearchToSqlConversion.searchToSql("tableName", input));
    }
}
