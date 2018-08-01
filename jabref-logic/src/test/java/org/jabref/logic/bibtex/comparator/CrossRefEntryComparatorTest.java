package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrossRefEntryComparatorTest {
    private CrossRefEntryComparator comparator;

    @BeforeEach
    public void setUp() {
        comparator = new CrossRefEntryComparator();
    }

    @AfterEach
    public void tearDown() {
        comparator = null;
    }

    @Test
    public void isEqualForEntriesWithoutCrossRef() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        assertEquals(0, comparator.compare(e1, e2));
    }

    @Test
    public void isEqualForEntriesWithCrossRef() {
        BibEntry e1 = new BibEntry();
        e1.setField(FieldName.CROSSREF, "1");
        BibEntry e2 = new BibEntry();
        e2.setField(FieldName.CROSSREF, "2");
        assertEquals(0, comparator.compare(e1, e2));
    }

    @Test
    public void isGreaterForEntriesWithoutCrossRef() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = new BibEntry();
        e2.setField(FieldName.CROSSREF, "1");
        assertEquals(1, comparator.compare(e1, e2));
    }

    @Test
    public void isSmallerForEntriesWithCrossRef() {
        BibEntry e1 = new BibEntry();
        e1.setField(FieldName.CROSSREF, "1");
        BibEntry e2 = new BibEntry();
        assertEquals(-1, comparator.compare(e1, e2));
    }
}
