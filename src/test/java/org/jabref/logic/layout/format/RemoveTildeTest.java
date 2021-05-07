package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveTildeTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveTilde();
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void formatText(String formattedString, String originalString) {
        assertEquals(formattedString, formatter.format(originalString));
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("simple", "simple"),
                Arguments.of(" ", "~"),
                Arguments.of("   ", "~~~"),
                Arguments.of(" \\~ ", "~\\~~"),
                Arguments.of("\\\\ ", "\\\\~"),
                Arguments.of("Doe Joe and Jane, M. and Kamp, J. A.", "Doe Joe and Jane, M. and Kamp, J.~A."),
                Arguments.of("T\\~olkien, J. R. R.", "T\\~olkien, J.~R.~R.")
        );
    }
}
