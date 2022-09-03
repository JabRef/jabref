package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ISSNCleanupTest {

    private CleanupWorker worker;

    @BeforeEach
    public void setUp() {
        worker = new CleanupWorker(
                mock(BibDatabaseContext.class),
                mock(FilePreferences.class),
                mock(TimestampPreferences.class));
    }

    @Test
    public void cleanupISSNReturnsCorrectISSN() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ISSN, "0123-4567");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("0123-4567"), entry.getField(StandardField.ISSN));
    }

    @Test
    public void cleanupISSNAddsMissingDash() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ISSN, "01234567");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("0123-4567"), entry.getField(StandardField.ISSN));
    }

    @Test
    public void cleanupISSNJunkStaysJunk() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ISSN, "Banana");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("Banana"), entry.getField(StandardField.ISSN));
    }
}
