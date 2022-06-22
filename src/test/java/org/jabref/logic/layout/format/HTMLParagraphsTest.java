package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HTMLParagraphsTest {

    LayoutFormatter htmlFormatter = new HTMLParagraphs();

    @ParameterizedTest
    @MethodSource("htmlFormatTests")
    void testCorrectFormat(String expectedString, String inputString) {
        assertEquals(expectedString, htmlFormatter.format(inputString));
    }

    private static Stream<Arguments> htmlFormatTests() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("<p>\nHello\n</p>", "Hello"),
                Arguments.of("<p>\nHello\nWorld\n</p>", "Hello\nWorld"),
                Arguments.of("<p>\nHello World\n</p>\n<p>\nWhat a lovely day\n</p>", "Hello World\n   \nWhat a lovely day\n"),
                Arguments.of("<p>\nHello World\n</p>\n<p>\nCould not be any better\n</p>\n<p>\nWhat a lovely day\n</p>", "Hello World\n \n\nCould not be any better\n\nWhat a lovely day\n")
        );
    }
}
