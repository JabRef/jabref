package org.jabref.logic.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~* 'compute'), title=compute"
    })
    void conversion(String expected, String input) {
        assertEquals(expected, SearchToSqlConversion.searchToSql("tableName", input));
    }
}
