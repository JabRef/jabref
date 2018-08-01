package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntryLinkCheckerTest {

    private BibDatabase database;
    private EntryLinkChecker checker;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        database = new BibDatabase();
        checker = new EntryLinkChecker(database);
        entry = new BibEntry();
        database.insertEntry(entry);
    }

    @Test
    public void testEntryLinkChecker() {
        assertThrows(NullPointerException.class, () -> new EntryLinkChecker(null));
    }

    @Test
    public void testCheckNoFields() {
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void testCheckNonRelatedFieldsOnly() {
        entry.setField("year", "2016");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void testCheckNonExistingCrossref() {
        entry.setField("crossref", "banana");

        List<IntegrityMessage> message = checker.check(entry);
        assertFalse(message.toString(), message.isEmpty());
    }

    @Test
    public void testCheckExistingCrossref() {
        entry.setField("crossref", "banana");

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("banana");
        database.insertEntry(entry2);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(Collections.emptyList(), message);
    }

    @Test
    public void testCheckExistingRelated() {
        entry.setField("related", "banana,pineapple");

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("banana");
        database.insertEntry(entry2);

        BibEntry entry3 = new BibEntry();
        entry3.setCiteKey("pineapple");
        database.insertEntry(entry3);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(Collections.emptyList(), message);
    }

    @Test
    public void testCheckNonExistingRelated() {
        BibEntry entry1 = new BibEntry();
        entry1.setField("related", "banana,pineapple,strawberry");
        database.insertEntry(entry1);

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("banana");
        database.insertEntry(entry2);

        BibEntry entry3 = new BibEntry();
        entry3.setCiteKey("pineapple");
        database.insertEntry(entry3);

        List<IntegrityMessage> message = checker.check(entry1);
        assertFalse(message.toString(), message.isEmpty());
    }
}
