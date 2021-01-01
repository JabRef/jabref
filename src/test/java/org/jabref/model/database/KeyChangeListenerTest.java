package org.jabref.model.database;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyChangeListenerTest {

    private BibDatabase db;
    private BibEntry entry1;
    private BibEntry entry2;
    private BibEntry entry3;
    private BibEntry entry4;

    @BeforeEach
    public void setUp() {
        db = new BibDatabase();

        entry1 = new BibEntry();
        entry1.setCitationKey("Entry1");
        entry1.setField(StandardField.CROSSREF, "Entry4");
        db.insertEntry(entry1);

        entry2 = new BibEntry();
        entry2.setCitationKey("Entry2");
        entry2.setField(StandardField.RELATED, "Entry1,Entry3");
        db.insertEntry(entry2);

        entry3 = new BibEntry();
        entry3.setCitationKey("Entry3");
        entry3.setField(StandardField.RELATED, "Entry1,Entry2,Entry3");
        db.insertEntry(entry3);

        entry4 = new BibEntry();
        entry4.setCitationKey("Entry4");
        db.insertEntry(entry4);
    }

    @Test
    public void testCrossrefChanged() {
        assertEquals(Optional.of("Entry4"), entry1.getField(StandardField.CROSSREF));
        entry4.setCitationKey("Banana");
        assertEquals(Optional.of("Banana"), entry1.getField(StandardField.CROSSREF));
    }

    @Test
    public void testRelatedChanged() {
        assertEquals(Optional.of("Entry1,Entry3"), entry2.getField(StandardField.RELATED));
        entry1.setCitationKey("Banana");
        assertEquals(Optional.of("Banana,Entry3"), entry2.getField(StandardField.RELATED));
    }

    @Test
    public void testRelatedChangedInSameEntry() {
        assertEquals(Optional.of("Entry1,Entry2,Entry3"), entry3.getField(StandardField.RELATED));
        entry3.setCitationKey("Banana");
        assertEquals(Optional.of("Entry1,Entry2,Banana"), entry3.getField(StandardField.RELATED));
    }

    @Test
    public void testCrossrefRemoved() {
        entry4.clearField(InternalField.KEY_FIELD);
        assertEquals(Optional.empty(), entry1.getField(StandardField.CROSSREF));
    }

    @Test
    public void testCrossrefEntryRemoved() {
        db.removeEntry(entry4);
        assertEquals(Optional.empty(), entry1.getField(StandardField.CROSSREF));
    }

    @Test
    public void testRelatedEntryRemoved() {
        db.removeEntry(entry1);
        assertEquals(Optional.of("Entry3"), entry2.getField(StandardField.RELATED));
    }

    @Test
    public void testRelatedAllEntriesRemoved() {
        List<BibEntry> entries = Arrays.asList(entry1, entry3);
        db.removeEntries(entries);
        assertEquals(Optional.empty(), entry2.getField(StandardField.RELATED));
    }
}
