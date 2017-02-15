package net.sf.jabref.logic.bibtex.comparator;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntryComparatorTest {
    @Test
    public void recognizeIdenticObjectsAsEqual() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = e1;
        assertEquals(0, new EntryComparator(false, false, "").compare(e1, e2));
    }
}
