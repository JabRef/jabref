package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitBibEntryDiffTest {

    @Test
    void noDifferences_shouldProduceEmptyFieldChanges() {
        BibEntry base = new BibEntry().withField(StandardField.TITLE, "My Title");
        GitBibEntryDiff diff = new GitBibEntryDiff(base, base, base);

        assertTrue(diff.getFieldChanges().isEmpty());
        assertFalse(diff.hasConflicts());
    }

    @Test
    void localAndRemoteAreEqual_differentFromBase_shouldNotBeConflict() {
        BibEntry base = new BibEntry().withField(StandardField.AUTHOR, "John Doe");
        BibEntry local = new BibEntry().withField(StandardField.AUTHOR, "Jane Smith");
        BibEntry remote = new BibEntry().withField(StandardField.AUTHOR, "Jane Smith");

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertFalse(diff.hasConflicts());
        assertEquals("Jane Smith", getResolvedValue(diff, StandardField.AUTHOR));
    }

    @Test
    void localEqualsBase_remoteChanged_shouldNotBeConflict() {
        BibEntry base = new BibEntry().withField(StandardField.AUTHOR, "John Doe");
        BibEntry local = new BibEntry().withField(StandardField.AUTHOR, "John Doe");
        BibEntry remote = new BibEntry().withField(StandardField.AUTHOR, "Jane Smith");

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertFalse(diff.hasConflicts());
        assertEquals("Jane Smith", getResolvedValue(diff, StandardField.AUTHOR));
    }

    @Test
    void remoteEqualsBase_localChanged_shouldNotBeConflict() {
        BibEntry base = new BibEntry().withField(StandardField.AUTHOR, "John Doe");
        BibEntry local = new BibEntry().withField(StandardField.AUTHOR, "Jane Smith");
        BibEntry remote = new BibEntry().withField(StandardField.AUTHOR, "John Doe");

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertFalse(diff.hasConflicts());
        assertEquals("Jane Smith", getResolvedValue(diff, StandardField.AUTHOR));
    }

    @Test
    void localAndRemoteDifferentFromBaseAndEachOther_shouldBeConflict() {
        BibEntry base = new BibEntry().withField(StandardField.AUTHOR, "John Doe");
        BibEntry local = new BibEntry().withField(StandardField.AUTHOR, "Jane Smith");
        BibEntry remote = new BibEntry().withField(StandardField.AUTHOR, "Michael Bay");

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertTrue(diff.hasConflicts());
        assertNull(getResolvedValue(diff, StandardField.AUTHOR));
    }

    @Test
    void allNullValues_shouldProduceNoFieldChange() {
        BibEntry base = new BibEntry();
        BibEntry local = new BibEntry();
        BibEntry remote = new BibEntry();

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertTrue(diff.getFieldChanges().isEmpty());
        assertFalse(diff.hasConflicts());
    }

    @Test
    void newFieldInLocalOnly_shouldBeAddedWithoutConflict() {
        BibEntry base = new BibEntry();
        BibEntry local = new BibEntry().withField(StandardField.YEAR, "2023");
        BibEntry remote = new BibEntry();

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertFalse(diff.hasConflicts());
        assertEquals("2023", getResolvedValue(diff, StandardField.YEAR));
    }

    @Test
    void newFieldWithDifferentValuesInLocalAndRemote_shouldBeConflict() {
        BibEntry base = new BibEntry();
        BibEntry local = new BibEntry().withField(StandardField.JOURNAL, "Nature");
        BibEntry remote = new BibEntry().withField(StandardField.JOURNAL, "Science");

        GitBibEntryDiff diff = new GitBibEntryDiff(base, local, remote);

        assertTrue(diff.hasConflicts());
        assertNull(getResolvedValue(diff, StandardField.JOURNAL));
    }

    // Utility
    private String getResolvedValue(GitBibEntryDiff diff, StandardField field) {
        GitBibEntryDiff.FieldChange fieldChange = diff.getFieldChanges().get(field);
        return fieldChange != null ? fieldChange.getResolvedValue() : null;
    }
}
