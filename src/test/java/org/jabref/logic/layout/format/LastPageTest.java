package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LastPageTest {

    private LayoutFormatter lastPageLayoutFormatter = new LastPage();

    @ParameterizedTest
    @MethodSource("provideArguments")
    void formatLastPage(String formattedText, String originalText) {
        assertEquals(formattedText, lastPageLayoutFormatter.format(originalText));
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("", null),
                Arguments.of("345", "345"),
                Arguments.of("350", "345-350"),
                Arguments.of("350", "345--350"),
                Arguments.of("", "--")
        );

    }
}
