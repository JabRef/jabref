package org.jabref.model.entry;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalBibEntryTest {

    @Test
    void canonicalRepresentationIsCorrectForStringMonth() {
        BibEntry entry = new BibEntry();
        entry.setMonth(Month.MAY);
        assertEquals("@misc{,\n" +
                "  month = {#may#}\n" +
                "}", CanonicalBibEntry.getCanonicalRepresentation(entry));
    }

    @Test
    public void simpleCanonicalRepresentation() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setCiteKey("key");
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "def");
        e.setField(StandardField.JOURNAL, "hij");
        String canonicalRepresentation = CanonicalBibEntry.getCanonicalRepresentation(e);
        assertEquals("@article{key,\n  author = {abc},\n  journal = {hij},\n  title = {def}\n}",
                canonicalRepresentation);
    }

    @Test
    public void canonicalRepresentationWithNewlines() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setCiteKey("key");
        e.setField(StandardField.ABSTRACT, "line 1\nline 2");
        String canonicalRepresentation = CanonicalBibEntry.getCanonicalRepresentation(e);
        assertEquals("@article{key,\n  abstract = {line 1\nline 2}\n}", canonicalRepresentation);
    }
}
