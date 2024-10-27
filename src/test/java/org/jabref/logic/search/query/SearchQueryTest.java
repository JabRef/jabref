package org.jabref.logic.search.query;

import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchQueryTest {
    @ParameterizedTest
    @CsvSource({
            "term",
            "term1 term2",
            "term1 term2 term3",
            "term1 AND term2",
            "term1 and term2",
            "term1 OR term2 and term3",
            "term1 and (term2 or term3)",
            "term1 and (term2 or term3) and term4",
            "NOT term1",
            "NOT (term1 AND term2)",
            "\"term\"",
            "\"term1 term2\"",
            "Breitenb{\\\"{u}}cher",
            "K{\\'{a}}lm{\\'{a}}n K{\\'{e}}pes",
            "field = value",
            "filed CONTAINS value",
            "field MATCHES value",
            "field != value",
            "field == value",
            "field !== value",
            "field =~ value",
            "field !=~ value",
            "field =! value",
            "field ==! value",
            "field =~! value",
            "field !=~! value",
            "field = \"value\"",
            "field = value1 AND field = value2",
            "(field = value1) AND (field = value2)",
            "field = Breitenb{\\\"{u}}cher",
            "field = \"value 1 value2\"",
            "\\!term",
            "t\\~erm",
            "t\\(1\\)erm",
            "t\\\"erm",
    })
    public void validSearchQuery(String searchExpression) {
        assertTrue(new SearchQuery(searchExpression).isValid());
    }

    @ParameterizedTest
    @CsvSource({
            "!term", // =!~() should be escaped with a backslash
            "t~erm",
            "t(erm",
            "term AND",
            "field CONTAINS NOT value",
    })
    public void invalidSearchQuery(String searchExpression) {
        assertFalse(new SearchQuery(searchExpression).isValid());
    }
}
