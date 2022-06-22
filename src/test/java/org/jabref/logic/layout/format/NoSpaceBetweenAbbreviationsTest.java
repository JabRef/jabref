package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoSpaceBetweenAbbreviationsTest {

    private LayoutFormatter nsbaLayoutFormatter = new NoSpaceBetweenAbbreviations();

    @ParameterizedTest
    @MethodSource("provideAbbreviations")
    void formatAbbreviations(String formattedAbbreviation, String originalAbbreviation) {
        assertEquals(formattedAbbreviation, nsbaLayoutFormatter.format(originalAbbreviation));
    }

    private static Stream<Arguments> provideAbbreviations() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("John Meier", "John Meier"),
                Arguments.of("J.F. Kennedy", "J. F. Kennedy"),
                Arguments.of("J.R.R. Tolkien", "J. R. R. Tolkien"),
                Arguments.of("J.R.R. Tolkien and J.F. Kennedy", "J. R. R. Tolkien and J. F. Kennedy")
        );
    }
}
