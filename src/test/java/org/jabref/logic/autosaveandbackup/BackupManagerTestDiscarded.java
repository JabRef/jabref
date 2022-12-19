package org.jabref.logic.autosaveandbackup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for "discarded" flag
 */
class BackupManagerTestDiscarded {

    private BibDatabaseContext bibDatabaseContext;
    private BackupManager backupManager;
    private Path testBib;
    private SavePreferences savePreferences;
    private PreferencesService preferencesService;
    private BibEntryTypesManager bibEntryTypesManager;

    @BeforeEach
    public void setup(@TempDir Path tempDir) throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);

        testBib = tempDir.resolve("test.bib");

        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(testBib);

        bibEntryTypesManager = new BibEntryTypesManager();

        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(savePreferences.shouldMakeBackup()).thenReturn(false);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.withMakeBackup(anyBoolean())).thenReturn(savePreferences);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);

        preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
        when(preferencesService.getSavePreferences()).thenReturn(savePreferences);

        saveDatabase();

        backupManager = new BackupManager(bibDatabaseContext, bibEntryTypesManager, preferencesService);
        makeBackup();
    }

    private void saveDatabase() throws IOException {
        try (Writer writer = new AtomicFileWriter(testBib, StandardCharsets.UTF_8, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            new BibtexDatabaseWriter(bibWriter, preferencesService.getGeneralPreferences(), savePreferences, bibEntryTypesManager)
                    .saveDatabase(bibDatabaseContext);
        }
    }

    private void databaseModification() {
        bibDatabaseContext.getDatabase().insertEntry(new BibEntry().withField(StandardField.NOTE, "test"));
    }

    private void makeBackup() {
        backupManager.determineBackupPathForNewBackup().ifPresent(backupManager::performBackup);
    }

    @Test
    public void noDiscardingAChangeLeadsToNewerBackupBeReported() throws Exception {
        databaseModification();
        makeBackup();
        assertTrue(BackupManager.backupFileDiffers(testBib));
    }

    @Test
    public void noDiscardingASavedChange() throws Exception {
        databaseModification();
        makeBackup();
        saveDatabase();
        assertFalse(BackupManager.backupFileDiffers(testBib));
    }

    @Test
    public void discardingAChangeLeadsToNewerBackupToBeIgnored() throws Exception {
        databaseModification();
        makeBackup();
        backupManager.discardBackup();
        assertFalse(BackupManager.backupFileDiffers(testBib));
    }
}
