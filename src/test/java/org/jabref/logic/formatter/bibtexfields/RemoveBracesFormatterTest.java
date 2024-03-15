package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class RemoveBracesFormatterTest {

    private RemoveBracesFormatter formatter = new RemoveBracesFormatter();

    @ParameterizedTest
    @MethodSource
    public void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> format() {
        return Stream.of(
                // formatRemovesSingleEnclosingBraces
                Arguments.of("test", "{test}"),

                // formatKeepsUnmatchedBracesAtBeginning
                Arguments.of("{test", "{test"),

                // formatKeepsUnmatchedBracesAtEnd
                Arguments.of("test}", "test}"),

                // formatKeepsShortString
                Arguments.of("t", "t"),

                // formatRemovesBracesOnly
                Arguments.of("", "{}"),

                // formatKeepsEmptyString
                Arguments.of("", ""),

                // formatRemovesDoubleEnclosingBraces
                Arguments.of("test", "{{test}}"),

                // formatRemovesTripleEnclosingBraces
                Arguments.of("test", "{{{test}}}"),

                // formatKeepsNonMatchingBraces
                Arguments.of("{A} and {B}", "{A} and {B}"),

                // formatRemovesOnlyMatchingBraces
                Arguments.of("{A} and {B}", "{{A} and {B}}"),

                // formatDoesNotRemoveBracesInBrokenString
                Arguments.of("{A} and {B}}", "{A} and {B}}"),

                Arguments.of("Vall{\\'e}e Poussin", "{Vall{\\'e}e Poussin}"),
                Arguments.of("Vall{\\'e}e Poussin", "Vall{\\'e}e Poussin")
        );
    }

    @Test
    public void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }
}
