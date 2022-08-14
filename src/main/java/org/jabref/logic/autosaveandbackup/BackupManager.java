package org.jabref.logic.autosaveandbackup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backups the given bib database file from {@link BibDatabaseContext} on every {@link BibDatabaseContextChangedEvent}.
 * An intelligent {@link ExecutorService} with a {@link BlockingQueue} prevents a high load while making backups and
 * rejects all redundant backup tasks. This class does not manage the .bak file which is created when opening a
 * database.
 */
public class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    private static final int MAXIMUM_BACKUP_FILE_COUNT = 10;

    private static Set<BackupManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final PreferencesService preferences;
    private final DelayTaskThrottler throttler;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;


    // Contains a list of all backup paths
    // During a write, the less recent backup file is deleted
    private final Queue<Path> backupFilesQueue = new LinkedBlockingQueue<>();

    private BackupManager(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, PreferencesService preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.throttler = new DelayTaskThrottler(15_000);

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);
    }

    /**
     * Determines the most recent backup file name
     */
    static Path getBackupPathForNewBackup(Path originalPath) {
        return BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalPath, BackupFileType.BACKUP);
    }

    /**
     * Determines the most recent existing backup file name
     */
    static Optional<Path> getLatestBackupPath(Path originalPath) {
        return BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP);
    }

    /**
     * Starts the BackupManager which is associated with the given {@link BibDatabaseContext}. As long as no database
     * file is present in {@link BibDatabaseContext}, the {@link BackupManager} will do nothing.
     *
     * This method is not thread-safe. The caller has to ensure that this method is not called in parallel.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static BackupManager start(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, PreferencesService preferences) {
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
     * Checks whether a backup file exists for the given database file. If it exists, it is checked whether it is
     * different from the original.
     *
     * @param originalPath Path to the file a backup should be checked for. Example: jabref.bib.
     * @return <code>true</code> if backup file exists AND differs from originalPath. <code>false</code> is the
     * "default" return value in the good case. In the case of an exception <code>true</code> is returned to ensure that
     * the user checks the output.
     */
    public static boolean backupFileDiffers(Path originalPath) {
        Optional<Path> backupPath = getLatestBackupPath(originalPath);
        if (backupPath.isEmpty()) {
            return false;
        }

        try {
            return Files.mismatch(originalPath, backupPath.get()) != -1L;
        } catch (IOException e) {
            LOGGER.debug("Could not compare original file and backup file.", e);
            // User has to investigate in this case
            return true;
        }
    }

    /**
     * Restores the backup file by copying and overwriting the original one.
     *
     * @param originalPath Path to the file which should be equalized to the backup file.
     */
    public static void restoreBackup(Path originalPath) {
        Optional<Path> backupPath = getLatestBackupPath(originalPath);
        if (backupPath.isEmpty()) {
            LOGGER.error("There is no backup file");
            return;
        }
        try {
            Files.copy(backupPath.get(), originalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Error while restoring the backup file.", e);
        }
    }

    private Optional<Path> determineBackupPathForNewBackup() {
        return bibDatabaseContext.getDatabasePath().map(BackupManager::getBackupPathForNewBackup);
    }

    /**
     * This method is called as soon as the scheduler says: "Do the backup"
     *
     * <em>SIDE EFFECT: Deletes oldest backup file</em>
     *
     * @param backupPath the path where the library should be backed up to
     */
    private void performBackup(Path backupPath) {
        // We opted for "while" to delete backups in case there are more than 10
        while (backupFilesQueue.size() >= MAXIMUM_BACKUP_FILE_COUNT) {
            Path lessRecentBackupFile = backupFilesQueue.poll();
            try {
                Files.delete(lessRecentBackupFile);
            } catch (IOException e) {
                LOGGER.error("Could not delete backup file {}", lessRecentBackupFile, e);
            }
        }

        // code similar to org.jabref.gui.exporter.SaveDatabaseAction.saveDatabase
        GeneralPreferences generalPreferences = preferences.getGeneralPreferences();
        SavePreferences savePreferences = preferences.getSavePreferences()
                                                     .withMakeBackup(false);
        Charset encoding = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);
        // We want to have successful backups only
        // Thus, we do not use a plain "FileWriter", but the "AtomicFileWriter"
        // Example: What happens if one hard powers off the machine (or kills the jabref process) during the write of the backup?
        //          This MUST NOT create a broken backup file that then jabref wants to "restore" from?
        try (Writer writer = new AtomicFileWriter(backupPath, encoding, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager)
                    .saveDatabase(bibDatabaseContext);
            backupFilesQueue.add(backupPath);
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
            LOGGER.error("Error while saving to file {}", backupPath, e);
        }
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        if (!event.isFilteredOut()) {
            startBackupTask();
        }
    }

    private void startBackupTask() {
        fillQueue();

        // We need to determine the backup path on each action, because we use the timestamp in the filename
        throttler.schedule(() -> determineBackupPathForNewBackup().ifPresent(this::performBackup));
    }

    private void fillQueue() {
        Path backupDir = BackupFileUtil.getAppDataBackupDir();
        if (!Files.exists(backupDir)) {
            return;
        }
        bibDatabaseContext.getDatabasePath().ifPresent(databasePath -> {
            // code similar to {@link org.jabref.logic.util.io.BackupFileUtil.getPathOfLatestExisingBackupFile}
            final String prefix = BackupFileUtil.getUniqueFilePrefix(databasePath) + "--" + databasePath.getFileName();
            try {
                List<Path> allSavFiles = Files.list(backupDir)
                                              // just list the .sav belonging to the given targetFile
                                              .filter(p -> p.getFileName().toString().startsWith(prefix))
                                              .sorted().toList();
                backupFilesQueue.addAll(allSavFiles);
            } catch (IOException e) {
                LOGGER.error("Could not determine most recent file", e);
            }
        });
    }

    /**
     * Unregisters the BackupManager from the eventBus of {@link BibDatabaseContext} and deletes the backup file. This
     * method should only be used when closing a database/JabRef legally.
     */
    private void shutdown() {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        throttler.shutdown();
    }
}
