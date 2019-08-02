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
}
