package org.jabref.logic.bibtex.comparator;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ConflictResolutionTest {

    @Test
    void resolveGitConflicts_shouldInsertMergedEntryWithoutConflict() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Old Title");
        BibEntry localEntry = new BibEntry().withField(StandardField.TITLE, "New Title");
        BibEntry remoteEntry = new BibEntry().withField(StandardField.TITLE, "New Title");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(baseEntry);
        localContext.getDatabase().insertEntry(localEntry);
        remoteContext.getDatabase().insertEntry(remoteEntry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs);

        boolean mergedEntryFound = localContext.getDatabase().getEntries().stream()
                                               .anyMatch(e -> "New Title".equals(e.getField(StandardField.TITLE).orElse(null)));

        assertTrue(mergedEntryFound, "Merged entry with 'New Title' should be present");
        assertEquals(4, localContext.getDatabase().getEntries().size(), "Should contain 4 entries after merge");
    }

    @Test
    void resolveGitConflicts_remoteChanged_localSameAsBase_shouldPreserveRemoteChange() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Original Title");
        BibEntry localEntry = new BibEntry().withField(StandardField.TITLE, "Original Title");
        BibEntry remoteEntry = new BibEntry().withField(StandardField.TITLE, "Updated Title");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(baseEntry);
        localContext.getDatabase().insertEntry(localEntry);
        remoteContext.getDatabase().insertEntry(remoteEntry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs);

        boolean remoteChangePreserved = localContext.getDatabase().getEntries().stream()
                                                    .anyMatch(e -> "Updated Title".equals(e.getField(StandardField.TITLE).orElse(null)));

        assertTrue(remoteChangePreserved, "Remote change should be preserved in merged result");
    }

    @Test
    void resolveGitConflicts_localChanged_remoteSameAsBase_shouldPreserveLocalChange() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Title");
        BibEntry localEntry = new BibEntry().withField(StandardField.TITLE, "Local Edit");
        BibEntry remoteEntry = new BibEntry().withField(StandardField.TITLE, "Title");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(baseEntry);
        localContext.getDatabase().insertEntry(localEntry);
        remoteContext.getDatabase().insertEntry(remoteEntry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs);

        boolean localChangePreserved = localContext.getDatabase().getEntries().stream()
                                                   .anyMatch(e -> "Local Edit".equals(e.getField(StandardField.TITLE).orElse(null)));

        assertTrue(localChangePreserved, "Local change should be preserved in merged result");
    }

    @Test
    void resolveGitConflicts_conflictDetected_shouldNotAutoMerge() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Original");
        BibEntry localEntry = new BibEntry().withField(StandardField.TITLE, "Local Change");
        BibEntry remoteEntry = new BibEntry().withField(StandardField.TITLE, "Remote Change");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(baseEntry);
        localContext.getDatabase().insertEntry(localEntry);
        remoteContext.getDatabase().insertEntry(remoteEntry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs);

        long mergedCount = localContext.getDatabase().getEntries().stream()
                                       .filter(e -> e.getField(StandardField.TITLE).orElse("").contains("Change"))
                                       .count();

        assertTrue(mergedCount < 2, "Conflicting entries should not both be merged blindly");
    }

    @Test
    void resolveGitConflicts_noChangeAcrossVersions_shouldDoNothing() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Same Title");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(entry);
        localContext.getDatabase().insertEntry(entry);
        remoteContext.getDatabase().insertEntry(entry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs);

        assertEquals(1, localContext.getDatabase().getEntries().size(), "No additional entries should be added if all are the same");
    }

    @Test
    void resolveGitConflicts_missingFieldInLocal_shouldNotCrash() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Base");
        BibEntry localEntry = new BibEntry(); // No title
        BibEntry remoteEntry = new BibEntry().withField(StandardField.TITLE, "Base");

        BibDatabaseContext baseContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext localContext = new BibDatabaseContext(new BibDatabase());
        BibDatabaseContext remoteContext = new BibDatabaseContext(new BibDatabase());

        baseContext.getDatabase().insertEntry(baseEntry);
        localContext.getDatabase().insertEntry(localEntry);
        remoteContext.getDatabase().insertEntry(remoteEntry);

        GuiPreferences mockPrefs = mock(GuiPreferences.class);

        assertDoesNotThrow(() -> ConflictResolution.resolveGitConflicts(baseContext, localContext, remoteContext, mockPrefs),
                "Should handle missing field gracefully");
    }
}
