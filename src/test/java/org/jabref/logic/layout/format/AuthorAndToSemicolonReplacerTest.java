package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorAndToSemicolonReplacerTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("Someone, Van Something", "Someone, Van Something"),
                Arguments.of("John Smith and Black Brown, Peter", "John Smith; Black Brown, Peter"),
                Arguments.of("von Neumann, John and Smith, John and Black Brown, Peter", "von Neumann, John; Smith, John; Black Brown, Peter"),
                Arguments.of("John von Neumann and John Smith and Peter Black Brown", "John von Neumann; John Smith; Peter Black Brown"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testFormat(String input, String expected) {
        LayoutFormatter a = new AuthorAndToSemicolonReplacer();

        assertEquals(expected, a.format(input));
    }
}
