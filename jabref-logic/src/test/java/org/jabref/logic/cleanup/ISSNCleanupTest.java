package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ISSNCleanupTest {

    private CleanupWorker worker;

    @BeforeEach
    public void setUp() {
        worker = new CleanupWorker(mock(BibDatabaseContext.class),
                new CleanupPreferences("", "", mock(LayoutFormatterPreferences.class),
                        mock(FileDirectoryPreferences.class)));
    }

    @Test
    public void cleanupISSNReturnsCorrectISSN() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField("issn", "0123-4567");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("0123-4567"), entry.getField("issn"));
    }

    @Test
    public void cleanupISSNAddsMissingDash() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField("issn", "01234567");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("0123-4567"), entry.getField("issn"));
    }

    @Test
    public void cleanupISSNJunkStaysJunk() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField("issn", "Banana");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("Banana"), entry.getField("issn"));
    }

}
