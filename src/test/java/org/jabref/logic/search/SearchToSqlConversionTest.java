package org.jabref.logic.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            // Default search, query without any field name (case insensitive contains)
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer'), computer",
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer science'), \"computer science\"",      // Phrase search
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer') OR (field_value ~* 'science'), computer science",     // Should be searched as a phrase or as two separate words (OR)?
            "SELECT entry_id FROM tableName WHERE (field_value ~* '!computer'), !computer",         // Is the explanation should be escaped?
            "SELECT entry_id FROM tableName WHERE (field_value ~* '!computer'), \"!computer\"",
            // search in all fields case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_value ~ 'computer'), any=!computer",
            "SELECT entry_id FROM tableName WHERE (field_value ~ '!computer'), any=! !computer",  // Is the explanation should be escaped?
            // Regex search
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'Jabref.*Search'), Jabref.*Search",
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'Jabref.*Search'), \"Jabref.*Search\"",   // This is wrong, this query should be a string literal searching for "Jabref.*Search" as string, should use syntax LIKE %...%
            // And
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer') AND (field_value ~* 'science'), computer AND science",
            // Or
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer') OR (field_value ~* 'science'), computer OR science",
            // Grouping
            "SELECT entry_id FROM tableName WHERE ((field_value ~* 'computer') AND (field_value ~* 'science')) OR (field_value ~* 'math'), (computer AND science) OR math",
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'computer') AND ((field_value ~* 'science') OR (field_value ~* 'math')), computer AND (science OR math)",
            "SELECT entry_id FROM tableName WHERE ((field_value ~* 'computer') OR (field_value ~* 'science')) AND ((field_value ~* 'math') OR (field_value ~* 'physics')), (computer OR science) AND (math OR physics)",
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
