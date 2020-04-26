package org.jabref.model.database;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicationCheckerTest {

    private BibDatabase database;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();
    }

    @Test
    void addEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    void addAndRemoveEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));
        database.removeEntry(entry);
        assertEquals(0, database.getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    void changeCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));
        entry.setCiteKey("BBB");
        assertEquals(0, database.getNumberOfKeyOccurrences("AAA"));
        assertEquals(1, database.getNumberOfKeyOccurrences("BBB"));
    }

    @Test
    void setCiteKeySameKeyDifferentEntries() {
        BibEntry entry0 = new BibEntry();
        entry0.setCiteKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCiteKey("BBB");
        database.insertEntry(entry1);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));
        assertEquals(1, database.getNumberOfKeyOccurrences("BBB"));

        entry1.setCiteKey("AAA");
        assertEquals(2, database.getNumberOfKeyOccurrences("AAA"));
        assertEquals(0, database.getNumberOfKeyOccurrences("BBB"));
    }

    @Test
    void removeMultipleCiteKeys() {
        BibEntry entry0 = new BibEntry();
        entry0.setCiteKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCiteKey("AAA");
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("AAA");
        database.insertEntry(entry2);
        assertEquals(3, database.getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry2);
        assertEquals(2, database.getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry1);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));

        database.removeEntry(entry0);
        assertEquals(0, database.getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    void addEmptyCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("");
        database.insertEntry(entry);

        assertEquals(0, database.getNumberOfKeyOccurrences(""));
    }

    @Test
    void removeEmptyCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfKeyOccurrences("AAA"));

        entry.setCiteKey("");
        database.removeEntry(entry);
        assertEquals(0, database.getNumberOfKeyOccurrences("AAA"));
    }
}
