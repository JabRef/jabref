package org.jabref.logic.search;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LuceneQueryParserTest {

    public static Stream<Arguments> searchQuires() {
        return Stream.of(
                // unicode
                Arguments.of("preissinger", "preißinger"),
                Arguments.of("jesus", "jesús"),
                Arguments.of("breitenbucher", "breitenbücher"),

                // latex
                Arguments.of("preissinger", "prei{\\ss}inger"),
                Arguments.of("jesus", "jes{\\'{u}}s"),
                Arguments.of("breitenbucher", "breitenb{\\\"{u}}cher")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchQuires(String expected, String query) {
        expected = "(all:" + expected + ")^4.0";
        SearchQuery searchQuery = new SearchQuery(query, EnumSet.noneOf(SearchFlags.class));
        assertEquals(expected, searchQuery.getParsedQuery().toString());
    }
}
