package org.jabref.logic.search;

import org.jabref.logic.search.query.SearchToSqlConversion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            // Default search, query without any field name (case insensitive contains)
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer%'), computer",
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer science%'), \"computer science\"",      // Phrase search
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer%') OR (field_value ILIKE '%science%'), computer science",     // Should be searched as a phrase or as two separate words (OR)?
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%!computer%'), !computer",         // Is the explanation should be escaped?
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%!computer%'), \"!computer\"",
            // search in all fields case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_value LIKE '%computer%'), any=!computer",
            "SELECT entry_id FROM tableName WHERE (field_value LIKE '%!computer%'), any=! !computer",  // Is the explanation should be escaped?
            // Regex search
            "SELECT entry_id FROM tableName WHERE (field_value ~* 'Jabref.*Search'), any=~Jabref.*Search",
            // And
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer%') AND (field_value ILIKE '%science%'), computer AND science",
            // Or
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer%') OR (field_value ILIKE '%science%'), computer OR science",
            // Grouping
            "SELECT entry_id FROM tableName WHERE ((field_value ILIKE '%computer%') AND (field_value ILIKE '%science%')) OR (field_value ILIKE '%math%'), (computer AND science) OR math",
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%computer%') AND ((field_value ILIKE '%science%') OR (field_value ILIKE '%math%')), computer AND (science OR math)",
            "SELECT entry_id FROM tableName WHERE ((field_value ILIKE '%computer%') OR (field_value ILIKE '%science%')) AND ((field_value ILIKE '%math%') OR (field_value ILIKE '%physics%')), (computer OR science) AND (math OR physics)",
            // case insensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ILIKE '%compute%'), title=compute",
            // case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value LIKE '%compute%'), title=!compute",
            // exact match case insensitive
            // "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~* '\\ycompute\\y'), title==compute",
            // exact match case sensitive
            // "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value ~ '\\ycompute\\y'), title==!compute",
            // negated case insensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value NOT ILIKE '%compute%'), title!=compute",
            // negated case sensitive contains
            "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value NOT LIKE '%compute%'), title !=! compute",
            // negated case insensitive exact match
            // "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~* '\\ycompute\\y'), title !== compute",
            // negated case sensitive exact match
            // "SELECT entry_id FROM tableName WHERE (field_name = 'title' AND field_value !~ '\\ycompute\\y'), title !==! compute",

            // Special characters
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%{IEEE}%'), {IEEE}",
            "SELECT entry_id FROM tableName WHERE (field_name = 'author' AND field_value ILIKE '%{IEEE}%'), author={IEEE}",
            // R\"ock
             "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%R\\\"ock%'), R\\\"ock",
            // Breitenb{\"{u}}cher
            "SELECT entry_id FROM tableName WHERE (field_value ILIKE '%Breitenb{\\\"{u}}cher%'), Breitenb{\\\"{u}}cher",
    })

    void conversion(String expected, String input) {
        assertEquals(expected, SearchToSqlConversion.searchToSql("tableName", input));
    }
}
