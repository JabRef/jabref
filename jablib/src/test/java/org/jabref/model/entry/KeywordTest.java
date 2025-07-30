package org.jabref.model.entry;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordTest {

    private static Stream<Arguments> provideParseKeywordCases() {
        return Stream.of(
                Arguments.of("keyword\\,one"),
                Arguments.of("keywordOne\\,"),
                Arguments.of("keyword\\\\"),
                Arguments.of("keyword\\,one > sub"),
                Arguments.of("one\\,two > three"),
                Arguments.of("keywordOne\\\\")
        );
    }

    @Test
    void getPathFromRootAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        assertEquals("A > B", keywordChain.getChild().get().getPathFromRootAsString('>'));
    }

    @Test
    void getAllSubchainsAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        Set<String> expected = new HashSet<>();
        expected.add("A");
        expected.add("A > B");
        expected.add("A > B > C");

        assertEquals(expected, keywordChain.getAllSubchainsAsString('>'));
    }

    @ParameterizedTest
    @MethodSource("provideParseKeywordCases")
    void getSubchainAsString(String input) {
        Keyword keyword = KeywordList.parse(input, ',', '>').get(0);
        // we are testing toString() functionality
        assertEquals(input, keyword.toString());
    }
}
