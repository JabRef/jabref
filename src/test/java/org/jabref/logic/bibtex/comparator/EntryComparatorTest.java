package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryComparatorTest {
    @Test
    void recognizeIdenticObjectsAsEqual() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = e1;
        assertEquals(0, new EntryComparator(false, false, StandardField.TITLE).compare(e1, e2));
    }

    @Test
    void compareAuthorFieldBiggerAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.AUTHOR, "Stephen King");
        entry2.setField(StandardField.AUTHOR, "Henning Mankell");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.AUTHOR);

        assertEquals(-2, entryComparator.compare(entry1, entry2));
    }

    @Test
    void bothEntriesHaveNotSetTheFieldToCompareAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.BOOKTITLE, "Stark - The Dark Half (1989)");
        entry2.setField(StandardField.COMMENTATOR, "Some Commentator");
        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void secondEntryHasNotSetFieldToCompareAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.TITLE, "Stark - The Dark Half (1989)");
        entry2.setField(StandardField.COMMENTATOR, "Some Commentator");
        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void firstEntryHasNotSetFieldToCompareAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.COMMENTATOR, "Some Commentator");
        entry2.setField(StandardField.TITLE, "Stark - The Dark Half (1989)");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.TITLE);

        assertEquals(1, entryComparator.compare(entry1, entry2));
    }

    @Test
    void bothEntriesNumericAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.EDITION, "1");
        entry2.setField(StandardField.EDITION, "3");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.EDITION);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }
/*
    @Test
    void firstEntryNotNumericAscending() throws Exception {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();

        entry1.setField(StandardField.EDITION, "3");
        entry2.setField(StandardField.EDITION, "1.5");

        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.EDITION);

        assertEquals(-1, entryComparator.compare(entry1, entry2));
    }*/
}
