package org.jabref.logic.autosaveandbackup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.jabref.gui.util.DelayTaskThrottler;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backups the given bib database file from {@link BibDatabaseContext} on every {@link BibDatabaseContextChangedEvent}.
 * An intelligent {@link ExecutorService} with a {@link BlockingQueue} prevents a high load while making backups and
 * rejects all redundant backup tasks.
 * This class does not manage the .bak file which is created when opening a database.
 */
public class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    private static final String BACKUP_EXTENSION = ".sav";

    private static Set<BackupManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final JabRefPreferences preferences;
    private final DelayTaskThrottler throttler;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;

    private BackupManager(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, JabRefPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.throttler = new DelayTaskThrottler(15000);

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);
    }

    static Path getBackupPath(Path originalPath) {
        return FileUtil.addExtension(originalPath, BACKUP_EXTENSION);
    }

    /**
     * Starts the BackupManager which is associated with the given {@link BibDatabaseContext}.
     * As long as no database file is present in {@link BibDatabaseContext}, the {@link BackupManager} will do nothing.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static BackupManager start(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, JabRefPreferences preferences) {
        BackupManager backupManager = new BackupManager(bibDatabaseContext, entryTypesManager, preferences);
        backupManager.startBackupTask();
        runningInstances.add(backupManager);
        return backupManager;
    }

    /**
     * Shuts down the BackupManager which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(
                BackupManager::shutdown);
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    /**
     * Checks whether a backup file exists for the given database file.
     *
     * @param originalPath Path to the file a backup should be checked for.
     */
    public static boolean checkForBackupFile(Path originalPath) {
        Path backupPath = getBackupPath(originalPath);
        return Files.exists(backupPath) && !Files.isDirectory(backupPath);
    }

    /**
     * Restores the backup file by copying and overwriting the original one.
     *
     * @param originalPath Path to the file which should be equalized to the backup file.
     */
    public static void restoreBackup(Path originalPath) {
        Path backupPath = getBackupPath(originalPath);
        try {
            Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Error while restoring the backup file.", e);
        }
    }

    private Optional<Path> determineBackupPath() {
        return bibDatabaseContext.getDatabasePath().map(BackupManager::getBackupPath);
    }

    private void performBackup(Path backupPath) {
        try {
            Charset charset = bibDatabaseContext.getMetaData().getEncoding().orElse(preferences.getDefaultEncoding());
            SavePreferences savePreferences = preferences.loadForSaveFromPreferences().withEncoding
                    (charset).withMakeBackup(false);
            new BibtexDatabaseWriter(new AtomicFileWriter(backupPath, savePreferences.getEncoding()), savePreferences, entryTypesManager)
                    .saveDatabase(bibDatabaseContext);
        } catch (IOException e) {
            logIfCritical(backupPath, e);
        }
    }

    private void logIfCritical(Path backupPath, IOException e) {
        Throwable innermostCause = e;
        while (innermostCause.getCause() != null) {
            innermostCause = innermostCause.getCause();
        }
        boolean isErrorInField = innermostCause instanceof InvalidFieldValueException;

        // do not print errors in field values into the log during autosave
        if (!isErrorInField) {
            LOGGER.error("Error while saving to file" + backupPath, e);
        }
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        startBackupTask();
    }

    private void startBackupTask() {
        throttler.schedule(() -> determineBackupPath().ifPresent(this::performBackup));
    }

    /**
     * Unregisters the BackupManager from the eventBus of {@link BibDatabaseContext} and deletes the backup file.
     * This method should only be used when closing a database/JabRef legally.
     */
    private void shutdown() {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        throttler.shutdown();
        determineBackupPath().ifPresent(this::deleteBackupFile);
    }

    private void deleteBackupFile(Path backupPath) {
        try {
            if (Files.exists(backupPath) && !Files.isDirectory(backupPath)) {
                Files.delete(backupPath);
            }
        } catch (IOException e) {
            LOGGER.error("Error while deleting the backup file.", e);
        }
    }
}
