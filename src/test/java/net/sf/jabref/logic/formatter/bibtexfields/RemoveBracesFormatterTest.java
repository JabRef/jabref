package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;


public class RemoveBracesFormatterTest {

    @Test
    public void formatRemovesSingleEnclosingBraces() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("test", formatter.format("{test}"));
    }

    @Test
    public void formatKeepsUnmatchedBracesAtBeginning() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("{test", formatter.format("{test"));
    }

    @Test
    public void formatKeepsUnmatchedBracesAtEnd() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("test}", formatter.format("test}"));
    }

    @Test
    public void formatKeepsShortString() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("t", formatter.format("t"));
    }

    @Test
    public void formatKeepsEmptyString() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("", formatter.format(""));
    }

    @Test
    public void formatRemovesDoubleEnclosingBraces() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("test", formatter.format("{{test}}"));
    }

    @Test
    public void formatRemovesTripleEnclosingBraces() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("test", formatter.format("{{{test}}}"));
    }

    @Test
    public void formatKeepsNonMatchingBraces() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("{A} and {B}", formatter.format("{A} and {B}"));
    }

    @Test
    public void formatRemovesOnlyMatchingBraces() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        assertEquals("{A} and {B}", formatter.format("{{A} and {B}}"));
    }

    @Test
    public void formatDoesNotRemoveBracesInBrokenString() {
        RemoveBracesFormatter formatter = new RemoveBracesFormatter();
        // We opt here for a conservative approach although one could argue that "A} and {B}" is also a valid return
        assertEquals("{A} and {B}}", formatter.format("{A} and {B}}"));
    }
}