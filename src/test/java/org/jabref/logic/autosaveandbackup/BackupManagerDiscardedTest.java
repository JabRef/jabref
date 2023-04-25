package org.jabref.logic.autosaveandbackup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;
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
class BackupManagerDiscardedTest {

    private BibDatabaseContext bibDatabaseContext;
    private BackupManager backupManager;
    private Path testBib;
    private SaveConfiguration saveConfiguration;
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

        saveConfiguration = mock(SaveConfiguration.class);
        when(saveConfiguration.shouldMakeBackup()).thenReturn(false);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        when(saveConfiguration.withMakeBackup(anyBoolean())).thenReturn(saveConfiguration);

        preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);

        saveDatabase();

        backupManager = new BackupManager(bibDatabaseContext, bibEntryTypesManager, preferencesService);
        makeBackup();
    }

    private void saveDatabase() throws IOException {
        try (Writer writer = new AtomicFileWriter(testBib, StandardCharsets.UTF_8, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    preferencesService.getFieldPreferences(),
                    preferencesService.getCitationKeyPatternPreferences(),
                    bibEntryTypesManager)
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
