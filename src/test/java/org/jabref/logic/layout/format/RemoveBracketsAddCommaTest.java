package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveBracketsAddCommaTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveBracketsAddComma();
    }

    @ParameterizedTest
    @MethodSource("provideExamples")
    void formatTextWithBrackets(String formattedString, String originalString) {
        assertEquals(formattedString, formatter.format(originalString));
    }

    private static Stream<Arguments> provideExamples() {
        return Stream.of(
                Arguments.of("some text,", "{some text}"),
                Arguments.of("some text", "{some text"),
                Arguments.of("some text,", "some text}")
        );
    }
}
