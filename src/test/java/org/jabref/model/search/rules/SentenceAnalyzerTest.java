package org.jabref.model.search.rules;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SentenceAnalyzerTest {

    static Stream<Arguments> getParameters() {
        return Stream.of(
                Arguments.of(List.of("a", "b"), "a b"),

                // Leading and trailing spaces
                Arguments.of(List.of("a", "b"), " a b "),

                // Escaped characters and trailing spaces
                Arguments.of(List.of("b "), "\"b \" "),

                // Escaped characters and leading spaces.
                Arguments.of(List.of(" a"), " \\ a")
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetWords(List<String> expected, String input) {
        assertEquals(expected, new SentenceAnalyzer(input).getWords());
    }
}
