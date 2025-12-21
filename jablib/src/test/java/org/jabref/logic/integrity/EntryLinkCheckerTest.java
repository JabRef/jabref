package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ResourceLock("Localization.lang")
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
    void checkNoFields() {
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkNonRelatedFieldsOnly() {
        entry.setField(StandardField.YEAR, "2016");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkNonExistingCrossref() {
        entry.setField(StandardField.CROSSREF, "banana");

        List<IntegrityMessage> message = checker.check(entry);
        assertFalse(message.isEmpty(), message.toString());
    }

    @Test
    void checkExistingCrossref() {
        entry.setField(StandardField.CROSSREF, "banana");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("banana");
        database.insertEntry(entry2);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(List.of(), message);
    }

    @Test
    void checkExistingRelated() {
        entry.setField(StandardField.RELATED, "banana,pineapple");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("banana");
        database.insertEntry(entry2);

        BibEntry entry3 = new BibEntry();
        entry3.setCitationKey("pineapple");
        database.insertEntry(entry3);

        List<IntegrityMessage> message = checker.check(entry);
        assertEquals(List.of(), message);
    }

    @Test
    void checkNonExistingRelated() {
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
