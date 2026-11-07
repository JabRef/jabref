package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchQueryTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "",
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

    // [utest->req~search.doi-link-normalization~1]
    @ParameterizedTest
    @ValueSource(strings = {
            "https://doi.org/10.1000/182",
            "http://doi.org/10.1000/182",
            "https://dx.doi.org/10.1000/182",
            "http://dx.doi.org/10.1000/182",
            "doi.org/10.1000/182",
            "doi:10.1000/182"
    })
    void doiNormalization(String searchExpression) {
        SearchQuery query = new SearchQuery(searchExpression);
        assertEquals("\"10.1000/182\"<EOF>", query.getContext().getText());
        assertEquals(searchExpression, query.getSearchExpression());
        assertEquals("\"10.1000/182\"", query.getNormalizedSearchExpression());
    }

    // [utest->req~search.doi-link-normalization~1]
    @Test
    void doiLikeImplicitAndNotNormalized() {
        SearchQuery query = new SearchQuery("10.1000/foo bar");
        assertEquals("10.1000/foo bar", query.getNormalizedSearchExpression());
    }

    @Test
    void doiLinkInMultiTermQueryIsNotNormalized() {
        String expression = "https://doi.org/10.1000/182\tother";
        assertEquals(expression, new SearchQuery(expression).getNormalizedSearchExpression());
    }

    @Test
    void doiLinkWithEncodedBackslashIsValid() {
        SearchQuery query = new SearchQuery("https://doi.org/10.1000/foo%5C");
        assertTrue(query.isValid());
        assertEquals("10.1000/foo\\", query.getHighlightSearchExpression());
    }

    @ParameterizedTest
    @MethodSource("highlightSearchExpressions")
    void highlightExpressionUsesDecodedDoi(String input, String expected) {
        assertEquals(expected, new SearchQuery(input).getHighlightSearchExpression());
    }

    private static Stream<Arguments> highlightSearchExpressions() {
        return Stream.of(
                Arguments.of("https://doi.org/10.1000/foo%2Bbar", "10.1000/foo+bar"),
                Arguments.of("https://doi.org/bfr%68mx", "bfrhmx"),
                Arguments.of("author=Alice", "author=Alice"));
    }

    // [utest->req~search.doi-link-normalization~1]
    @Test
    void doiNotNormalizedInRegexMode() {
        SearchQuery query = new SearchQuery("https://doi.org/10.1000/foo\\.", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        assertEquals("https://doi.org/10.1000/foo\\.", query.getNormalizedSearchExpression());
        assertEquals("https://doi.org/10.1000/foo\\.", query.getHighlightSearchExpression());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "!term", // =!~() should be escaped with a backslash
            "t~erm",
            "t(erm",
            "term AND",
            "field CONTAINS NOT value",
    })
    public void invalidSearchQuery(String searchExpression) {
        assertFalse(new SearchQuery(searchExpression).isValid());
    }

    private static Stream<Arguments> validRegularExpressionSearchQuery() {
        return Stream.of(
                Arguments.of("term.*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of("field =~ term.*", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of("field !=~ term.*", EnumSet.noneOf(SearchFlags.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void validRegularExpressionSearchQuery(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        assertTrue(new SearchQuery(searchExpression, searchFlags).isValid());
    }

    private static Stream<Arguments> invalidRegularExpressionSearchQuery() {
        return Stream.of(
                Arguments.of("*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of("field =~ *", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of("field !=~ [", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of("field =~ \\", EnumSet.noneOf(SearchFlags.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void invalidRegularExpressionSearchQuery(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        assertFalse(new SearchQuery(searchExpression, searchFlags).isValid());
    }
}
