package org.jabref.logic.search.query;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchQueryExtractorConversionTest {
    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of(List.of("term"), "term"),
                Arguments.of(List.of("regex.*term"), "regex.*term"),
                Arguments.of(List.of("term"), "any = term"),
                Arguments.of(List.of("term"), "any CONTAINS term"),
                Arguments.of(List.of("a", "b"), "a AND b"),
                Arguments.of(List.of("a", "b", "c"), "a OR b AND c"),
                Arguments.of(List.of("a", "b"), "a OR b AND NOT c"),
                Arguments.of(List.of("a", "b"), "author = a AND title = b"),
                Arguments.of(List.of(), "NOT a"),
                Arguments.of(List.of("a", "b", "c"), "(any = a OR any = b) AND NOT (NOT c AND title = d)"),
                Arguments.of(List.of("b", "c"), "title != a OR b OR c"),
                Arguments.of(List.of("a", "b"), "a b")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(List<String> expected, String searchExpression) {
        List<String> result = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression)).stream().map(SearchQueryNode::term).toList();
        assertEquals(expected, result);
    }
}
