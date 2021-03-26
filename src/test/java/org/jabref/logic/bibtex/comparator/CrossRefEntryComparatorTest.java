package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrossRefEntryComparatorTest {

    private static final CrossRefEntryComparator comparator = new CrossRefEntryComparator();

    @Test
    public void isEqualForEntriesWithoutCrossRef() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        assertEquals(0, comparator.compare(e1, e2));
    }

    @Test
    public void isEqualForEntriesWithCrossRef() {
        BibEntry e1 = new BibEntry();
        e1.setField(StandardField.CROSSREF, "1");
        BibEntry e2 = new BibEntry();
        e2.setField(StandardField.CROSSREF, "2");
        assertEquals(0, comparator.compare(e1, e2));
    }

    @Test
    public void isGreaterForEntriesWithoutCrossRef() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        e2.setField(StandardField.CROSSREF, "1");
        assertEquals(1, comparator.compare(e1, e2));
    }

    @Test
    public void isSmallerForEntriesWithCrossRef() {
        BibEntry e1 = new BibEntry();
        e1.setField(StandardField.CROSSREF, "1");
        BibEntry e2 = new BibEntry();
        assertEquals(-1, comparator.compare(e1, e2));
    }
}
