package org.jabref.logic.search;

import org.jabref.logic.search.query.SearchToSqlConversion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToSqlConversionTest {
    @ParameterizedTest
    @CsvSource({
            // case insensitive contains
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal ILIKE '%compute%') OR (main_table.field_value_transformed ILIKE '%compute%'))), title=compute",

            // case sensitive contains
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal LIKE '%compute%') OR (main_table.field_value_transformed LIKE '%compute%'))), title=!compute",

            // exact match case insensitive
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal ILIKE 'compute') OR (main_table.field_value_transformed ILIKE 'compute'))) OR ((split_table.field_name = 'title') AND ((split_table.field_value_literal ILIKE 'compute') OR (split_table.field_value_transformed ILIKE 'compute'))), title==compute",

            // exact match case sensitive
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal LIKE 'compute') OR (main_table.field_value_transformed LIKE 'compute'))) OR ((split_table.field_name = 'title') AND ((split_table.field_value_literal LIKE 'compute') OR (split_table.field_value_transformed LIKE 'compute'))), title==!compute",

            // Regex search case insensitive
            "(main_table.field_value_literal ~* 'Jabref.*Search') OR (main_table.field_value_transformed ~* 'Jabref.*Search'), any=~Jabref.*Search",

            // Regex search case sensitive
            "(main_table.field_value_literal ~ 'Jabref.*Search') OR (main_table.field_value_transformed ~ 'Jabref.*Search'), any=~!Jabref.*Search",

            // negated case insensitive contains
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal NOT ILIKE '%compute%') OR (main_table.field_value_transformed NOT ILIKE '%compute%'))), title!=compute",

            // negated case sensitive contains
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal NOT LIKE '%compute%') OR (main_table.field_value_transformed NOT LIKE '%compute%'))), title !=! compute",

            // negated case insensitive exact match
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal NOT ILIKE 'compute') OR (main_table.field_value_transformed NOT ILIKE 'compute'))) OR ((split_table.field_name = 'title') AND ((split_table.field_value_literal NOT ILIKE 'compute') OR (split_table.field_value_transformed NOT ILIKE 'compute'))), title !== compute",

            // negated case sensitive exact match
            "((main_table.field_name = 'title') AND ((main_table.field_value_literal NOT LIKE 'compute') OR (main_table.field_value_transformed NOT LIKE 'compute'))) OR ((split_table.field_name = 'title') AND ((split_table.field_value_literal NOT LIKE 'compute') OR (split_table.field_value_transformed NOT LIKE 'compute'))), title !==! compute",

            // negated regex search case insensitive
            "(main_table.field_value_literal !~* 'Jabref.*Search') OR (main_table.field_value_transformed !~* 'Jabref.*Search'), any!=~Jabref.*Search",

            // negated regex search case sensitive
            "(main_table.field_value_literal !~ 'Jabref.*Search') OR (main_table.field_value_transformed !~ 'Jabref.*Search'), any!=~!Jabref.*Search",

            // Default search, query without any field name (case insensitive contains)
            "(main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%'), computer",
            "(main_table.field_value_literal ILIKE '%computer science%') OR (main_table.field_value_transformed ILIKE '%computer science%'), \"computer science\"",      // Phrase search
            "(field_value ILIKE '%computer%') OR (field_value ILIKE '%science%'), computer science",     // Should be searched as a phrase or as two separate words (OR)? (Throw exception)
            "(field_value ILIKE '%!computer%'), !computer",         // Is the explanation should be escaped?  (Throw exception)
            "(main_table.field_value_literal ILIKE '%!computer%') OR (main_table.field_value_transformed ILIKE '%!computer%'), \"!computer\"",

            // search in all fields case sensitive contains
            "(main_table.field_value_literal LIKE '%computer%') OR (main_table.field_value_transformed LIKE '%computer%'), any=!computer",
            "(field_value LIKE '%!computer%'), any=! !computer",  // Is the explanation should be escaped? (Throw exception)

            // And
            "(main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%') AND (main_table.field_value_literal ILIKE '%science%') OR (main_table.field_value_transformed ILIKE '%science%'), computer AND science",

            // Or
            "(main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%') OR (main_table.field_value_literal ILIKE '%science%') OR (main_table.field_value_transformed ILIKE '%science%'), computer OR science",

            // Grouping
            "((main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%') AND (main_table.field_value_literal ILIKE '%science%') OR (main_table.field_value_transformed ILIKE '%science%')) OR (main_table.field_value_literal ILIKE '%math%') OR (main_table.field_value_transformed ILIKE '%math%'), (computer AND science) OR math",
            "(main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%') AND ((main_table.field_value_literal ILIKE '%science%') OR (main_table.field_value_transformed ILIKE '%science%') OR (main_table.field_value_literal ILIKE '%math%') OR (main_table.field_value_transformed ILIKE '%math%')), computer AND (science OR math)",
            "((main_table.field_value_literal ILIKE '%computer%') OR (main_table.field_value_transformed ILIKE '%computer%') OR (main_table.field_value_literal ILIKE '%science%') OR (main_table.field_value_transformed ILIKE '%science%')) AND ((main_table.field_value_literal ILIKE '%math%') OR (main_table.field_value_transformed ILIKE '%math%') OR (main_table.field_value_literal ILIKE '%physics%') OR (main_table.field_value_transformed ILIKE '%physics%')), (computer OR science) AND (math OR physics)",

            // Special characters
            "(main_table.field_value_literal ILIKE '%{IEEE}%') OR (main_table.field_value_transformed ILIKE '%{IEEE}%'), {IEEE}",
            "((main_table.field_name = 'author') AND ((main_table.field_value_literal ILIKE '%{IEEE}%') OR (main_table.field_value_transformed ILIKE '%{IEEE}%'))), author={IEEE}",

            // R\"ock
            "(field_value ILIKE '%R\\\"ock%'), R\\\"ock",  // (Throw exception)
            // Breitenb{\"{u}}cher
            "(field_value ILIKE '%Breitenb{\\\"{u}}cher%'), Breitenb{\\\"{u}}cher", // (Throw exception)
    })

    void conversion(String expectedWhereClause, String input) {
        assertEquals(expectedWhereClause, SearchToSqlConversion.getWhereClause("tableName", input));
    }
}
