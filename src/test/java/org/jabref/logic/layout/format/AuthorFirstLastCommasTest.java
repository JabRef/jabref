package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorFirstLastCommasTest {

    LayoutFormatter authorFLCFormatter = new AuthorFirstLastCommas();

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstLastCommas#format(java.lang.String)}.
     */
    @ParameterizedTest
    @MethodSource("formatTests")
    void paramLayoutFormatTest(String expectedString, String inputString) {
        assertEquals(expectedString, authorFLCFormatter.format(inputString));
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("Van Something Someone", "Someone, Van Something"),
                Arguments.of("John von Neumann and Peter Black Brown", "John von Neumann and Peter Black Brown"),
                Arguments.of("John von Neumann, John Smith and Peter Black Brown", "von Neumann, John and Smith, John and Black Brown, Peter"),
                Arguments.of("John von Neumann, John Smith and Peter Black Brown", "John von Neumann and John Smith and Black Brown, Peter"),
                Arguments.of("John von Neumann and Peter Black Brown", "John von Neumann and Peter Black Brown"),
                Arguments.of("John von Neumann and Peter Black Brown", "John von Neumann and Peter Black Brown")
        );
    }
}
