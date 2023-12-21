package org.jabref.preferences;

import java.util.stream.Stream;

import org.jabref.logic.push.CitationCommandString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationCommandStringTest {

    @Test
    void testToString() {
        assertEquals("\\cite{key1,key2}", new CitationCommandString("\\cite{", ",", "}").toString());
    }

    public static Stream<Arguments> from() {
        return Stream.of(
                Arguments.of(
                        new CitationCommandString("\\cite{", ",", "}"),
                        "\\cite{key1,key2}"
                ),
                Arguments.of(
                        new CitationCommandString("\\cite{", ",", "}"),
                        "\\cite"
                ),
                // We could do better, but this is very seldom
                Arguments.of(
                        new CitationCommandString("\\cite[key1,key]{", ",", "}"),
                        "\\cite[key1,key]"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void from(CitationCommandString expected, String input) {
        assertEquals(expected, CitationCommandString.from(input));
    }
}
