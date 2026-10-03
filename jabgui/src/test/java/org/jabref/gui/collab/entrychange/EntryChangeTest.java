package org.jabref.gui.collab.entrychange;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.CompoundEdit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryChangeTest {

    private BibEntry localEntry;
    private BibEntry diskEntry;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        localEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Einstein1905")
                .withField(StandardField.TITLE, "Old title")
                .withField(StandardField.YEAR, "1905")
                .withChanged(false);
        diskEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Einstein1905")
                .withField(StandardField.TITLE, "New title")
                .withField(StandardField.AUTHOR, "Einstein")
                .withUserComments("% revised");
        databaseContext = new BibDatabaseContext(new BibDatabase(List.of(localEntry)));
    }

    @Test
    void applyingKeepsTheEntryInstance() {
        new EntryChange(localEntry, diskEntry, databaseContext).applyChange(new CompoundEdit("Merge"));

        assertEquals(1, databaseContext.getDatabase().getEntryCount());
        assertSame(localEntry, databaseContext.getDatabase().getEntries().getFirst());
    }

    @Test
    void applyingTakesOverTheDiskContent() {
        new EntryChange(localEntry, diskEntry, databaseContext).applyChange(new CompoundEdit("Merge"));

        assertEquals(diskEntry, localEntry);
        assertTrue(localEntry.hasChanged());
    }

    @Test
    void undoingRestoresTheLocalContentAndChangedFlag() {
        BibEntry original = new BibEntry(localEntry);
        CompoundEdit edit = new CompoundEdit("Merge");
        new EntryChange(localEntry, diskEntry, databaseContext).applyChange(edit);

        assertTrue(edit.toChangeSet().inverted().apply().complete());

        assertEquals(original, localEntry);
        assertFalse(localEntry.hasChanged());
    }
}
