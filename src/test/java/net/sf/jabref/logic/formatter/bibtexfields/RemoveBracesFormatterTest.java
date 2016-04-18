package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class RemoveBracesFormatterTest {

    private final RemoveBracesFormatter formatter = new RemoveBracesFormatter();


    @Test
    public void formatRemovesSingleEnclosingBraces() {
        assertEquals("test", formatter.format("{test}"));
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
    public void formatRemovesDoubleEnclosingBraces() {
        assertEquals("test", formatter.format("{{test}}"));
    }

    @Test
    public void formatRemovesTripleEnclosingBraces() {
        assertEquals("test", formatter.format("{{{test}}}"));
    }

    @Test
    public void formatKeepsNonMatchingBraces() {
        assertEquals("{A} and {B}", formatter.format("{A} and {B}"));
    }

    @Test
    public void formatRemovesOnlyMatchingBraces() {
        assertEquals("{A} and {B}", formatter.format("{{A} and {B}}"));
    }

    @Test
    public void formatDoesNotRemoveBracesInBrokenString() {
        // We opt here for a conservative approach although one could argue that "A} and {B}" is also a valid return
        assertEquals("{A} and {B}}", formatter.format("{A} and {B}}"));
    }

    @Test
    public void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }

}