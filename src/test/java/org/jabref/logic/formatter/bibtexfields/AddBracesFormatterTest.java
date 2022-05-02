package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class AddBracesFormatterTest {

    private AddBracesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new AddBracesFormatter();
    }

    @Test
    public void formatAddsSingleEnclosingBraces() {
        assertEquals("{test}", formatter.format("test"));
    }

    @Test
    public void formatKeepsUnmatchedBracesAtBeginning() {
        assertEquals("{test", formatter.format("{test"));
    }

    @Test
    public void formatKeepsUnmatchedBracesAtEnd() {
        assertEquals("test}", formatter.format("test}"));
    }

    @Test
    public void formatKeepsShortString() {
        assertEquals("t", formatter.format("t"));
    }

    @Test
    public void formatKeepsEmptyString() {
        assertEquals("", formatter.format(""));
    }

    @Test
    public void formatKeepsDoubleEnclosingBraces() {
        assertEquals("{{test}}", formatter.format("{{test}}"));
    }

    @Test
    public void formatKeepsTripleEnclosingBraces() {
        assertEquals("{{{test}}}", formatter.format("{{{test}}}"));
    }

    @Test
    public void formatKeepsNonMatchingBraces() {
        assertEquals("{A} and {B}", formatter.format("{A} and {B}"));
    }

    @Test
    public void formatKeepsOnlyMatchingBraces() {
        assertEquals("{{A} and {B}}", formatter.format("{{A} and {B}}"));
    }

    @Test
    public void formatDoesNotRemoveBracesInBrokenString() {
        // We opt here for a conservative approach although one could argue that "A} and {B}" is also a valid return
        assertEquals("{A} and {B}}", formatter.format("{A} and {B}}"));
    }

    @Test
    public void formatExample() {
        assertEquals("{In CDMA}", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void formatStringWithMinimalRequiredLength() {
        assertEquals("{AB}", formatter.format("AB"));
    }
}
