package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test for "discarded" flag
 */
class BackupManagerDiscardedTest {

    private BibDatabaseContext bibDatabaseContext;
    private BackupManager backupManager;
    private Path testBib;
    private SelfContainedSaveConfiguration saveConfiguration;
    private CliPreferences preferences;
    private BibEntryTypesManager bibEntryTypesManager;
    private Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
        this.backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);

        testBib = tempDir.resolve("test.bib");

        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(testBib);

        bibEntryTypesManager = new BibEntryTypesManager();
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);

        saveDatabase();

        backupManager = new BackupManager(mock(LibraryTab.class), bibDatabaseContext, bibEntryTypesManager, preferences);

        makeBackup();
    }

    private void saveDatabase() throws IOException {
        try (Writer writer = new AtomicFileWriter(testBib, StandardCharsets.UTF_8, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            new BibDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    preferences.getFieldPreferences(),
                    preferences.getCitationKeyPatternPreferences(),
                    bibEntryTypesManager)
                    .saveDatabase(bibDatabaseContext);
        }
    }

    private void databaseModification() {
        bibDatabaseContext.getDatabase().insertEntry(new BibEntry().withField(StandardField.NOTE, "test"));
    }

    private void makeBackup() {
        backupManager.determineBackupPathForNewBackup(backupDir).ifPresent(path -> backupManager.performBackup(path));
    }

    @Test
    void noDiscardingAChangeLeadsToNewerBackupBeReported() {
        databaseModification();
        makeBackup();
        assertTrue(BackupManager.backupFileDiffers(testBib, backupDir));
    }

    @Test
    void noDiscardingASavedChange() throws IOException {
        databaseModification();
        makeBackup();
        saveDatabase();
        assertFalse(BackupManager.backupFileDiffers(testBib, backupDir));
    }

    @Test
    void discardingAChangeLeadsToNewerBackupToBeIgnored() {
        databaseModification();
        makeBackup();
        backupManager.discardBackup(backupDir);
        assertFalse(BackupManager.backupFileDiffers(testBib, backupDir));
    }
}
