package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class RemoveEnclosingBracesFormatterTest {

    private final RemoveEnclosingBracesFormatter formatter = new RemoveEnclosingBracesFormatter();

    @ParameterizedTest
    @CsvSource({
            "test, {test}", // formatRemovesSingleEnclosingBraces
            "{test, {test", // formatKeepsUnmatchedBracesAtBeginning
            "test}, test}", // formatKeepsUnmatchedBracesAtEnd
            "t, t", // formatKeepsShortString
            "'', {}", // formatRemovesBracesOnly
            "test, {{test}}", // formatKeepsEmptyString
            "test, {{{test}}}", // formatRemovesDoubleEnclosingBraces
            "{A} and {B}, {A} and {B}", // formatRemovesTripleEnclosingBraces
            "{A} and {B}, {{A} and {B}}", // formatKeepsNonMatchingBraces
            "{A} and {B}}, {A} and {B}}", // formatRemovesOnlyMatchingBraces
            "Vall{\\'e}e Poussin, {Vall{\\'e}e Poussin}", // formatDoesNotRemoveBracesInBrokenString
            "Vall{\\'e}e Poussin, Vall{\\'e}e Poussin"
    })
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }
}
