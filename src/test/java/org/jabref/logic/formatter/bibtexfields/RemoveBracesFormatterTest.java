package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class RemoveBracesFormatterTest {

    private RemoveBracesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveBracesFormatter();
    }

    @Test
    void formatRemovesSingleEnclosingBraces() {
        assertEquals("test", formatter.format("{test}"));
    }

    @Test
    void formatKeepsUnmatchedBracesAtBeginning() {
        assertEquals("{test", formatter.format("{test"));
    }

    @Test
    void formatKeepsUnmatchedBracesAtEnd() {
        assertEquals("test}", formatter.format("test}"));
    }

    @Test
    void formatKeepsShortString() {
        assertEquals("t", formatter.format("t"));
    }

    @Test
    void formatRemovesBracesOnly() {
        assertEquals("", formatter.format("{}"));
    }

    @Test
    void formatKeepsEmptyString() {
        assertEquals("", formatter.format(""));
    }

    @Test
    void formatRemovesDoubleEnclosingBraces() {
        assertEquals("test", formatter.format("{{test}}"));
    }

    @Test
    void formatRemovesTripleEnclosingBraces() {
        assertEquals("test", formatter.format("{{{test}}}"));
    }

    @Test
    void formatKeepsNonMatchingBraces() {
        assertEquals("{A} and {B}", formatter.format("{A} and {B}"));
    }

    @Test
    void formatRemovesOnlyMatchingBraces() {
        assertEquals("{A} and {B}", formatter.format("{{A} and {B}}"));
    }

    @Test
    void formatDoesNotRemoveBracesInBrokenString() {
        // We opt here for a conservative approach although one could argue that "A} and {B}" is also a valid return
        assertEquals("{A} and {B}}", formatter.format("{A} and {B}}"));
    }

    @Test
    void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }
}
