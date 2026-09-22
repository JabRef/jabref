package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchQueryExtractorConversionTest {
    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of(List.of(), "", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("term"), "term", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("regex\\.\\*term"), "regex.*term", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("regex.*term"), "regex.*term", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(List.of("term"), "any = term", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("term"), "any CONTAINS term", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("regex.*term"), "any =~ regex.*term", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b"), "a AND b", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b", "c"), "a OR b AND c", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b"), "a OR b AND NOT c", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b"), "author = a AND title = b", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(), "NOT a", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b", "c"), "(any = a OR any = b) AND NOT (NOT c AND title = d)", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("b", "c"), "title != a OR b OR c", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("a", "b"), "a b", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("term1 term2"), "\"term1 term2\"", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("t\\(1\\)erm"), "t\\(1\\)erm", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of("C:\\\\temp"), "\"C:\\temp\"", EnumSet.noneOf(SearchFlags.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(List<String> expected, String searchExpression, EnumSet<SearchFlags> searchFlags) {
        List<String> result = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression, searchFlags)).stream().map(SearchQueryNode::term).toList();
        assertEquals(expected, result);
    }
}
