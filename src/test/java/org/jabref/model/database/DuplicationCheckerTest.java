package org.jabref.model.database;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DuplicationCheckerTest {

    private BibDatabase database;


    @BeforeEach
    public void setUp() {
        database = new BibDatabase();
    }

    @Test
    public void addEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void addAndRemoveEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
        database.removeEntry(entry);
        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void changeCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
        entry.setCiteKey("BBB");
        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"));
    }


    @Test
    public void setCiteKeySameKeyDifferentEntries() {
        BibEntry entry0 = new BibEntry();
        entry0.setCiteKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCiteKey("BBB");
        database.insertEntry(entry1);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"));

        entry1.setCiteKey("AAA");
        assertEquals(2, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences("BBB"));
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
        assertEquals(3, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry2);
        assertEquals(2, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry1);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry0);
        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void addEmptyCiteKey(){
        BibEntry entry = new BibEntry();
        entry.setCiteKey("");
        database.insertEntry(entry);

        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences(""));
    }

    @Test
    public void removeEmptyCiteKey(){
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));

        entry.setCiteKey("");
        database.removeEntry(entry);
        assertEquals(0, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

}
