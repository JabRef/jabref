package org.jabref.logic.search.query;

import java.util.Set;
import java.util.stream.Stream;

import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchQueryExtractorConversionTest {
    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of(Set.of("term"), "term"),
                Arguments.of(Set.of("regex.*term"), "regex.*term"),
                Arguments.of(Set.of("term"), "any = term"),
                Arguments.of(Set.of("term"), "any CONTAINS term"),
                Arguments.of(Set.of("a", "b"), "a AND b"),
                Arguments.of(Set.of("a", "b", "c"), "a OR b AND c"),
                Arguments.of(Set.of("a", "b"), "a OR b AND NOT c"),
                Arguments.of(Set.of("a", "b"), "author = a AND title = b"),
                Arguments.of(Set.of(), "NOT a"),
                Arguments.of(Set.of("a", "b", "c"), "(any = a OR any = b) AND NOT (NOT c AND title = d)"),
                Arguments.of(Set.of("b", "c"), "title != a OR b OR c"),
                Arguments.of(Set.of("a", "b"), "a b")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(Set<String> expected, String searchExpression) {
        Set<String> result = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression));
        assertEquals(expected, result);
    }
}
