package org.jabref.model.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntrySorterTest {

    @Test
    public void testEmptyEntrySorter() throws Exception {
        EntrySorter es = new EntrySorter(Collections.emptyList(), Comparator.comparing(BibEntry::getId));
        assertEquals(0, es.getEntryCount());
    }

    @Test
    public void testEntrySorterWithOneElement() throws Exception {
        BibEntry entryA = new BibEntry("article");
        EntrySorter es = new EntrySorter(Collections.singletonList(entryA), Comparator.comparing(BibEntry::getId));
        assertEquals(1, es.getEntryCount());
        assertEquals(entryA, es.getEntryAt(0));
    }

    @Test
    public void testEntrySorterWithTwoElements() throws Exception {
        BibEntry entryB = new BibEntry("article");
        entryB.setId("2");
        BibEntry entryA = new BibEntry("article");
        entryB.setId("1");
        EntrySorter es = new EntrySorter(Arrays.asList(entryB, entryA), Comparator.comparing(BibEntry::getId));
        assertEquals(2, es.getEntryCount());
        assertEquals(entryA, es.getEntryAt(0));
        assertEquals(entryB, es.getEntryAt(1));
    }

}
