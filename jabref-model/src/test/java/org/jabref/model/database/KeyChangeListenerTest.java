package org.jabref.model.database;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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
        entry1.setCiteKey("Entry1");
        entry1.setField(FieldName.CROSSREF, "Entry4");
        db.insertEntry(entry1);

        entry2 = new BibEntry();
        entry2.setCiteKey("Entry2");
        entry2.setField(FieldName.RELATED, "Entry1,Entry3");
        db.insertEntry(entry2);

        entry3 = new BibEntry();
        entry3.setCiteKey("Entry3");
        entry3.setField(FieldName.RELATED, "Entry1,Entry2,Entry3");
        db.insertEntry(entry3);

        entry4 = new BibEntry();
        entry4.setCiteKey("Entry4");
        db.insertEntry(entry4);

    }

    @Test
    public void testCrossrefChanged() {
        assertEquals(Optional.of("Entry4"), entry1.getField("crossref"));
        entry4.setCiteKey("Banana");
        assertEquals(Optional.of("Banana"), entry1.getField("crossref"));
    }

    @Test
    public void testRelatedChanged() {
        assertEquals(Optional.of("Entry1,Entry3"), entry2.getField("related"));
        entry1.setCiteKey("Banana");
        assertEquals(Optional.of("Banana,Entry3"), entry2.getField("related"));
    }

    @Test
    public void testRelatedChangedInSameEntry() {
        assertEquals(Optional.of("Entry1,Entry2,Entry3"), entry3.getField("related"));
        entry3.setCiteKey("Banana");
        assertEquals(Optional.of("Entry1,Entry2,Banana"), entry3.getField("related"));
    }

    @Test
    public void testCrossrefRemoved() {
        entry4.clearField(BibEntry.KEY_FIELD);
        assertEquals(Optional.empty(), entry1.getField("crossref"));
    }

    @Test
    public void testCrossrefEntryRemoved() {
        db.removeEntry(entry4);
        assertEquals(Optional.empty(), entry1.getField("crossref"));
    }

    @Test
    public void testRelatedEntryRemoved() {
        db.removeEntry(entry1);
        assertEquals(Optional.of("Entry3"), entry2.getField("related"));
    }

    @Test
    public void testRelatedAllEntriesRemoved() {
        db.removeEntry(entry1);
        db.removeEntry(entry3);
        assertEquals(Optional.empty(), entry2.getField("related"));
    }

}
