package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryFlagsConversionTest {

    private static Stream<Arguments> testSearchConversion() {
        return Stream.of(
                createTestCases(
                        "Term",
                        "any = Term",
                        "any =! Term",
                        "any =~ Term",
                        "any =~! Term"
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
                        "(title = \"Tem\" AND author != Alex) OR any = term",
                        "(title =! \"Tem\" AND author !=! Alex) OR any =! term",
                        "(title =~ \"Tem\" AND author !=~ Alex) OR any =~ term",
                        "(title =~! \"Tem\" AND author !=~! Alex) OR any =~! term"
                )
        ).flatMap(stream -> stream);
    }

    private static Stream<Arguments> createTestCases(String query, String noneExpected, String caseSensitiveExpected, String regexExpected, String bothExpected) {
        return Stream.of(
                Arguments.of(noneExpected, query, EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(caseSensitiveExpected, query, EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                Arguments.of(regexExpected, query, EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(bothExpected, query, EnumSet.of(SearchFlags.CASE_SENSITIVE, SearchFlags.REGULAR_EXPRESSION))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSearchConversion(String expected, String query, EnumSet<SearchFlags> flags) {
        String result = SearchQueryConversion.flagsToSearchExpression(query, flags);
        assertEquals(expected, result);
    }
}
