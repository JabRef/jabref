package org.jabref.logic.search;

import org.jabref.logic.search.query.SearchToSqlConversion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            // case insensitive contains
            "(field_name = 'title' AND field_value ILIKE '%compute%'), title=compute",

            // case sensitive contains
            "(field_name = 'title' AND field_value LIKE '%compute%'), title=!compute",

            // exact match case insensitive
            "(field_name = 'title' AND field_value ILIKE 'compute'), title==compute",

            // exact match case sensitive
            "(field_name = 'title' AND field_value LIKE 'compute'), title==!compute",

            // Regex search case insensitive
            "(field_value ~* 'Jabref.*Search'), any=~Jabref.*Search",

            // Regex search case sensitive
            "(field_value ~ 'Jabref.*Search'), any=~!Jabref.*Search",

            // negated case insensitive contains
            "(field_name = 'title' AND field_value NOT ILIKE '%compute%'), title!=compute",

            // negated case sensitive contains
            "(field_name = 'title' AND field_value NOT LIKE '%compute%'), title !=! compute",

            // negated case insensitive exact match
            "(field_name = 'title' AND field_value NOT ILIKE 'compute'), title !== compute",

            // negated case sensitive exact match
            "(field_name = 'title' AND field_value NOT LIKE 'compute'), title !==! compute",

            // negated regex search case insensitive
            "(field_value !~* 'Jabref.*Search'), any!=~Jabref.*Search",

            // negated regex search case sensitive
            "(field_value !~ 'Jabref.*Search'), any!=~!Jabref.*Search",

            // Default search, query without any field name (case insensitive contains)
            "(field_value ILIKE '%computer%'), computer",
            "(field_value ILIKE '%computer science%'), \"computer science\"",      // Phrase search
            "(field_value ILIKE '%computer%') OR (field_value ILIKE '%science%'), computer science",     // Should be searched as a phrase or as two separate words (OR)?
            "(field_value ILIKE '%!computer%'), !computer",         // Is the explanation should be escaped?
            "(field_value ILIKE '%!computer%'), \"!computer\"",

            // search in all fields case sensitive contains
            "(field_value LIKE '%computer%'), any=!computer",
            "(field_value LIKE '%!computer%'), any=! !computer",  // Is the explanation should be escaped?

            // And
            "(field_value ILIKE '%computer%') AND (field_value ILIKE '%science%'), computer AND science",

            // Or
            "(field_value ILIKE '%computer%') OR (field_value ILIKE '%science%'), computer OR science",

            // Grouping
            "((field_value ILIKE '%computer%') AND (field_value ILIKE '%science%')) OR (field_value ILIKE '%math%'), (computer AND science) OR math",
            "(field_value ILIKE '%computer%') AND ((field_value ILIKE '%science%') OR (field_value ILIKE '%math%')), computer AND (science OR math)",
            "((field_value ILIKE '%computer%') OR (field_value ILIKE '%science%')) AND ((field_value ILIKE '%math%') OR (field_value ILIKE '%physics%')), (computer OR science) AND (math OR physics)",

            // Special characters
            "(field_value ILIKE '%{IEEE}%'), {IEEE}",
            "(field_name = 'author' AND field_value ILIKE '%{IEEE}%'), author={IEEE}",

            // R\"ock
            "(field_value ILIKE '%R\\\"ock%'), R\\\"ock",
            // Breitenb{\"{u}}cher
            "(field_value ILIKE '%Breitenb{\\\"{u}}cher%'), Breitenb{\\\"{u}}cher",
    })
    void conversion(String expected, String input) {
        assertEquals("SELECT entry_id FROM \"tableName\" WHERE " + expected + " GROUP BY entry_id", SearchToSqlConversion.searchToSql("tableName", input));
    }
}
