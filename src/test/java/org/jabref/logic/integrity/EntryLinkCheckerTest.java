package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntryLinkCheckerTest {

    private BibDatabase database;
    private EntryLinkChecker checker;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();
        checker = new EntryLinkChecker(database);
        entry = new BibEntry();
        database.insertEntry(entry);
    }

    @Test
    void testEntryLinkChecker() {
        assertThrows(NullPointerException.class, () -> new EntryLinkChecker(null));
    }

    @Test
    void testCheckNoFields() {
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void testCheckNonRelatedFieldsOnly() {
        entry.setField(StandardField.YEAR, "2016");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void testCheckNonExistingCrossref() {
        entry.setField(StandardField.CROSSREF, "banana");

        List<IntegrityMessage> message = checker.check(entry);
        assertFalse(message.isEmpty(), message.toString());
    }

    @Test
    void testCheckExistingCrossref() {
        entry.setField(StandardField.CROSSREF, "banana");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("banana");
        database.insertEntry(entry2);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(Collections.emptyList(), message);
    }

    @Test
    void testCheckExistingRelated() {
        entry.setField(StandardField.RELATED, "banana,pineapple");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("banana");
        database.insertEntry(entry2);

        BibEntry entry3 = new BibEntry();
        entry3.setCitationKey("pineapple");
        database.insertEntry(entry3);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(Collections.emptyList(), message);
    }

    @Test
    void testCheckNonExistingRelated() {
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.RELATED, "banana,pineapple,strawberry");
        database.insertEntry(entry1);

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("banana");
        database.insertEntry(entry2);

        BibEntry entry3 = new BibEntry();
        entry3.setCitationKey("pineapple");
        database.insertEntry(entry3);

        List<IntegrityMessage> message = checker.check(entry1);
        assertFalse(message.isEmpty(), message.toString());
    }
}
