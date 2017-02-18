package org.jabref.model.database;

import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DuplicationCheckerTest {

    private BibDatabase database;


    @Before
    public void setUp() {
        database = new BibDatabase();
    }

    @Test
    public void addEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);
    }

    @Test
    public void addAndRemoveEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);
        database.removeEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 0);
    }

    @Test
    public void changeCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);
        entry.setCiteKey("BBB");
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 0);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"), 1);
    }


    @Test
    public void setCiteKeySameKeyDifferentEntries() {
        BibEntry entry0 = new BibEntry();
        entry0.setCiteKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCiteKey("BBB");
        database.insertEntry(entry1);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"), 1);

        entry1.setCiteKey("AAA");
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 2);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"), 0);
    }

    @Test
    public void removeMultipleCiteKeys(){
        BibEntry entry0 = new BibEntry();
        entry0.setCiteKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCiteKey("AAA");
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("AAA");
        database.insertEntry(entry2);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 3);

        database.removeEntry(entry2);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 2);

        database.removeEntry(entry1);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);

        database.removeEntry(entry0);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 0);
    }

    @Test
    public void addEmptyCiteKey(){
        BibEntry entry = new BibEntry();
        entry.setCiteKey("");
        database.insertEntry(entry);

        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences(""), 0);
    }

    @Test
    public void removeEmptyCiteKey(){
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 1);

        entry.setCiteKey("");
        database.removeEntry(entry);
        assertEquals(database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"), 0);
    }

}
