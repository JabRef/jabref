package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class ISSNCleanupTest {

    private CleanupWorker worker;


    @Before
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
        Assert.assertEquals(Optional.of("0123-4567"), entry.getField("issn"));
    }

    @Test
    public void cleanupISSNAddsMissingDash() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField("issn", "01234567");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("0123-4567"), entry.getField("issn"));
    }

    @Test
    public void cleanupISSNJunkStaysJunk() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        BibEntry entry = new BibEntry();
        entry.setField("issn", "Banana");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("Banana"), entry.getField("issn"));
    }

}
