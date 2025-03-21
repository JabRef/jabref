package org.jabref.logic.bibtex.comparator;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitBibDatabaseDiffTest {

    @Test
    void noDifferencesShouldProduceEmptyGitEntryDiffs() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Same Title");

        BibDatabaseContext base = contextWith(entry);
        BibDatabaseContext local = contextWith(entry);
        BibDatabaseContext remote = contextWith(entry);

        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);

        assertEquals(0, diff.getEntryDifferences().size());
        assertTrue(diff.getMetaDataDifferences().isEmpty());
    }

    @Test
    void addedFieldInLocal_shouldBeDetected() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Old Title");
        BibEntry localEntry = ((BibEntry) baseEntry.clone()).withField(StandardField.AUTHOR, "Alice");

        BibDatabaseContext base = contextWith(baseEntry);
        BibDatabaseContext local = contextWith(localEntry);
        BibDatabaseContext remote = contextWith(baseEntry); // unchanged

        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);

        assertEquals(1, diff.getEntryDifferences().size());
        GitBibEntryDiff entryDiff = diff.getEntryDifferences().getFirst();

        assertEquals("Alice", entryDiff.localEntry().getField(StandardField.AUTHOR).orElse(null));
        assertFalse(entryDiff.hasConflicts()); // only local changed
    }

    @Test
    void conflictShouldBeDetectedBetweenLocalAndRemote() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Old Title");
        BibEntry localEntry = ((BibEntry) baseEntry.clone()).withField(StandardField.TITLE, "Local Title");
        BibEntry remoteEntry = ((BibEntry) baseEntry.clone()).withField(StandardField.TITLE, "Remote Title");

        baseEntry.setCitationKey("sharedKey");
        localEntry.setCitationKey("sharedKey");
        remoteEntry.setCitationKey("sharedKey");

        BibDatabaseContext base = contextWith(baseEntry);
        BibDatabaseContext local = contextWith(localEntry);
        BibDatabaseContext remote = contextWith(remoteEntry);

        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);
        GitBibEntryDiff entryDiff = diff.getEntryDifferences().getFirst();

        assertTrue(entryDiff.hasConflicts());
        assertEquals("Local Title", entryDiff.localEntry().getField(StandardField.TITLE).orElse(null));
        assertEquals("Remote Title", entryDiff.remoteEntry().getField(StandardField.TITLE).orElse(null));
    }

    @Test
    void entryOnlyChangedInRemote_shouldStillBeTracked() {
        BibEntry baseEntry = new BibEntry().withField(StandardField.TITLE, "Same");

        BibEntry remoteEntry = ((BibEntry) baseEntry.clone()).withField(StandardField.AUTHOR, "Bob");

        BibDatabaseContext base = contextWith(baseEntry);
        BibDatabaseContext local = contextWith(baseEntry); // unchanged
        BibDatabaseContext remote = contextWith(remoteEntry);

        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);
        GitBibEntryDiff entryDiff = diff.getEntryDifferences().getFirst();

        assertEquals("Bob", entryDiff.remoteEntry().getField(StandardField.AUTHOR).orElse(null));
        assertFalse(entryDiff.hasConflicts());
    }

    @Test
    void entriesMatchedByCitationKeyShouldWorkEvenWithDifferentIds() {
        BibEntry baseEntry = new BibEntry()
                .withField(StandardField.TITLE, "Base Title")
                .withCitationKey("MyKey");

        BibEntry localEntry = new BibEntry()
                .withField(StandardField.TITLE, "Base Title")
                .withCitationKey("MyKey");

        BibEntry remoteEntry = new BibEntry()
                .withField(StandardField.TITLE, "Remote Title")
                .withCitationKey("MyKey");

        BibDatabaseContext base = contextWith(baseEntry);
        BibDatabaseContext local = contextWith(localEntry);
        BibDatabaseContext remote = contextWith(remoteEntry);

        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);
        GitBibEntryDiff entryDiff = diff.getEntryDifferences().getFirst();

        assertEquals("MyKey", entryDiff.remoteEntry().getCitationKey().orElse(null));
        assertEquals("Remote Title", entryDiff.remoteEntry().getField(StandardField.TITLE).orElse(null));
    }

    private BibDatabaseContext contextWith(BibEntry... entries) {
        BibDatabase db = new BibDatabase();
        db.insertEntries(List.of(entries));
        return new BibDatabaseContext(db);
    }
}
