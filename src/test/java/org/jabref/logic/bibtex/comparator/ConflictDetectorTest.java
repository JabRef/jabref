package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConflictDetectorTest {

    @Test
    void testNoConflicts() {
        BibDatabaseContext localDb = new BibDatabaseContext();
        BibDatabaseContext remoteDb = new BibDatabaseContext();

        assertFalse(ConflictDetector.hasConflicts(localDb, remoteDb));
        assertTrue(ConflictDetector.detectConflicts(localDb, remoteDb).isEmpty());
        assertTrue(ConflictDetector.getEntryConflicts(localDb, remoteDb).isEmpty());
    }

    @Test
    void testEntryConflictsDetected() {
        BibEntry localEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Author, A")
                .withField(StandardField.TITLE, "JabRef Book");

        BibEntry remoteEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Author, B")
                .withField(StandardField.TITLE, "JabRef Book");

        BibDatabaseContext localDb = new BibDatabaseContext();
        BibDatabaseContext remoteDb = new BibDatabaseContext();
        localDb.getDatabase().insertEntry(localEntry);
        remoteDb.getDatabase().insertEntry(remoteEntry);

        Optional<BibDatabaseDiff> diff = ConflictDetector.detectConflicts(localDb, remoteDb);
        assertTrue(diff.isPresent());
        assertTrue(ConflictDetector.hasConflicts(localDb, remoteDb));

        List<BibEntryDiff> entryConflicts = ConflictDetector.getEntryConflicts(localDb, remoteDb);
        assertEquals(1, entryConflicts.size());
    }

    @Test
    void testMetadataConflictsDetected() {
        BibDatabaseContext localDb = new BibDatabaseContext();
        BibDatabaseContext remoteDb = new BibDatabaseContext();

        localDb.setMode(BibDatabaseMode.BIBTEX);
        remoteDb.setMode(BibDatabaseMode.BIBLATEX);

        assertTrue(ConflictDetector.hasConflicts(localDb, remoteDb));
        Optional<BibDatabaseDiff> diff = ConflictDetector.detectConflicts(localDb, remoteDb);
        assertTrue(diff.isPresent());
        assertTrue(diff.get().getMetaDataDifferences().isPresent());
    }
}
