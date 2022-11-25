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
        entry.setCitationKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));
    }

    @Test
    void addAndRemoveEntry() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));
        database.removeEntry(entry);
        assertEquals(0, database.getNumberOfCitationKeyOccurrences("AAA"));
    }

    @Test
    void changeCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));
        entry.setCitationKey("BBB");
        assertEquals(0, database.getNumberOfCitationKeyOccurrences("AAA"));
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("BBB"));
    }

    @Test
    void setCiteKeySameKeyDifferentEntries() {
        BibEntry entry0 = new BibEntry();
        entry0.setCitationKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCitationKey("BBB");
        database.insertEntry(entry1);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("BBB"));

        entry1.setCitationKey("AAA");
        assertEquals(2, database.getNumberOfCitationKeyOccurrences("AAA"));
        assertEquals(0, database.getNumberOfCitationKeyOccurrences("BBB"));
    }

    @Test
    void removeMultipleCiteKeys() {
        BibEntry entry0 = new BibEntry();
        entry0.setCitationKey("AAA");
        database.insertEntry(entry0);
        BibEntry entry1 = new BibEntry();
        entry1.setCitationKey("AAA");
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("AAA");
        database.insertEntry(entry2);
        assertEquals(3, database.getNumberOfCitationKeyOccurrences("AAA"));

        database.removeEntry(entry2);
        assertEquals(2, database.getNumberOfCitationKeyOccurrences("AAA"));

        database.removeEntry(entry1);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));

        database.removeEntry(entry0);
        assertEquals(0, database.getNumberOfCitationKeyOccurrences("AAA"));
    }

    @Test
    void addEmptyCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("");
        database.insertEntry(entry);

        assertEquals(0, database.getNumberOfCitationKeyOccurrences(""));
    }

    @Test
    void removeEmptyCiteKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getNumberOfCitationKeyOccurrences("AAA"));

        entry.setCitationKey("");
        database.removeEntry(entry);
        assertEquals(0, database.getNumberOfCitationKeyOccurrences("AAA"));
    }
}
