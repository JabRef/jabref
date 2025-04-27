package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryFlagsConversionTest {

    private static Stream<Arguments> searchConversion() {
        return Stream.of(
                createTestCases(
                        "Term",
                        "Term",
                        "Term",
                        "Term",
                        "Term"
                ),

                createTestCases(
                        "title = Term",
                        "title = Term",
                        "title =! Term",
                        "title =~ Term",
                        "title =~! Term"
                ),
                createTestCases(
                        "title == Term",
                        "title == Term",
                        "title ==! Term",
                        "title =~ Term",
                        "title =~! Term"
                ),

                createTestCases(
                        "title != Term",
                        "title != Term",
                        "title !=! Term",
                        "title !=~ Term",
                        "title !=~! Term"
                ),

                createTestCases(
                        "title = Tem AND author = Alex",
                        "title = Tem AND author = Alex",
                        "title =! Tem AND author =! Alex",
                        "title =~ Tem AND author =~ Alex",
                        "title =~! Tem AND author =~! Alex"
                ),

                createTestCases(
                        "(title = Tem) AND (author = Alex)",
                        "(title = Tem) AND (author = Alex)",
                        "(title =! Tem) AND (author =! Alex)",
                        "(title =~ Tem) AND (author =~ Alex)",
                        "(title =~! Tem) AND (author =~! Alex)"
                ),

                createTestCases(
                        "(title = \"Tem\" AND author != Alex) OR term",
                        "(title = \"Tem\" AND author != Alex) OR term",
                        "(title =! \"Tem\" AND author !=! Alex) OR term",
                        "(title =~ \"Tem\" AND author !=~ Alex) OR term",
                        "(title =~! \"Tem\" AND author !=~! Alex) OR term"
                )
        ).flatMap(stream -> stream);
    }

    private static Stream<Arguments> createTestCases(String searchExpression, String noneExpected, String caseSensitiveExpected, String regexExpected, String bothExpected) {
        return Stream.of(
                Arguments.of(noneExpected, searchExpression, EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(caseSensitiveExpected, searchExpression, EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                Arguments.of(regexExpected, searchExpression, EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(bothExpected, searchExpression, EnumSet.of(SearchFlags.CASE_SENSITIVE, SearchFlags.REGULAR_EXPRESSION))
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(String expected, String searchExpression, EnumSet<SearchFlags> flags) {
        String result = SearchQueryConversion.flagsToSearchExpression(new SearchQuery(searchExpression, flags));
        assertEquals(expected, result);
    }
}
