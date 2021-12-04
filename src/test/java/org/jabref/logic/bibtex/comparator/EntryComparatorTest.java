package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryComparatorTest {

    private BibEntry entry1 = new BibEntry();
    private BibEntry entry2 = new BibEntry();

    @Test
    void recognizeIdenticObjectsAsEqual() {
        BibEntry e2 = entry1;
        assertEquals(0, new EntryComparator(false, false, StandardField.TITLE).compare(entry1, e2));
    }

    @Test
    void compareAuthorFieldBiggerAscending() throws Exception {
        entry1.setField(StandardField.AUTHOR, "Stephen King");
        entry2.setField(StandardField.AUTHOR, "Henning Mankell");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.AUTHOR);

        assertEquals(-2, entryComparator.compare(entry1, entry2));
    }

    @Test
    void bothEntriesHaveNotSetTheFieldToCompareAscending() throws Exception {

        entry1.setField(StandardField.BOOKTITLE, "Stark - The Dark Half (1989)");
        entry2.setField(StandardField.COMMENTATOR, "Some Commentator");
        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void secondEntryHasNotSetFieldToCompareAscending() throws Exception {

        entry1.setField(StandardField.TITLE, "Stark - The Dark Half (1989)");
        entry2.setField(StandardField.COMMENTATOR, "Some Commentator");
        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void firstEntryHasNotSetFieldToCompareAscending() throws Exception {

        entry1.setField(StandardField.COMMENTATOR, "Some Commentator");
        entry2.setField(StandardField.TITLE, "Stark - The Dark Half (1989)");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void bothEntriesNumericAscending() throws Exception {

        entry1.setField(StandardField.EDITION, "1");
        entry2.setField(StandardField.EDITION, "3");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.EDITION);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void compareObjectsByKeyAscending() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        e1.setCitationKey("Mayer2019b");
        e2.setCitationKey("Mayer2019a");
        assertEquals(1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
        assertEquals(-1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e2, e1));
    }

    @Test
    void compareObjectsByKeyWithNull() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        e1.setCitationKey("Mayer2019b");
        assertEquals(-1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
        assertEquals(1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e2, e1));
    }

    @Test
    void compareObjectsByKeyWithBlank() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        e1.setCitationKey("Mayer2019b");
        e2.setCitationKey(" ");
        assertEquals(-1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
        assertEquals(1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e2, e1));
    }

    @Test
    void compareWithCrLfFields() {
        BibEntry e1 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibEntry e2 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        assertEquals(0, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
    }

    @Test
    void compareWithLfFields() {
        BibEntry e1 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry e2 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        assertEquals(0, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
    }

    @Test
    void compareWithMixedLineEndings() {
        BibEntry e1 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry e2 = new BibEntry()
                .withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        assertEquals(-1, new EntryComparator(false, false, InternalField.KEY_FIELD).compare(e1, e2));
    }
}


