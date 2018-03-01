package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class EntryComparatorTest {
    @Test
    public void recognizeIdenticObjectsAsEqual() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = e1;
        assertEquals(0, new EntryComparator(false, false, "").compare(e1, e2));
    }
}
