package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackUpManagerJGit {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);


    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Set<BackupManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;

    // Contains a list of all backup paths
    // During writing, the less recent backup file is deleted
    //private final Queue<Path> backupFilesQueue = new LinkedBlockingQueue<>();
    private boolean needsBackup = false;

    public BackUpManagerJGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);
    }
    /**
     * Starts the BackupManager which is associated with the given {@link BibDatabaseContext}. As long as no database
     * file is present in {@link BibDatabaseContext}, the {@link BackupManager} will do nothing.
     *
     * This method is not thread-safe. The caller has to ensure that this method is not called in parallel.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */

    public static BackUpManagerJGit start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        BackUpManagerJGit backupManagerJGit = new BackUpManagerJGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        backupManagerJGit.startBackupTask(preferences.getFilePreferences().getBackupDirectory());
        runningInstances.add(backupManagerJGit);
        return backupManagerJGit;
    }
    /**
     * Shuts down the BackupManager which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     * @param createBackup True, if a backup should be created
     * @param backupDir The path to the backup directory
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdown(backupDir, createBackup));
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }
    /**
     * Checks whether a backup file exists for the given database file. If it exists, it is checked whether it is
     * newer and different from the original.
     *
     * In case a discarded file is present, the method also returns <code>false</code>, See also {@link #discardBackup(Path)}.
     *
     * @param originalPath Path to the file a backup should be checked for. Example: jabref.bib.
     *
     * @return <code>true</code> if backup file exists AND differs from originalPath. <code>false</code> is the
     * "default" return value in the good case. In case a discarded file exists, <code>false</code> is returned, too.
     * In the case of an exception <code>true</code> is returned to ensure that the user checks the output.
     */
    public static boolean backupGitDiffers(Path originalPath, Path backupDir) {
        //à implementer
        Path discardedFile = determineDiscardedFile(originalPath, backupDir);
        if (Files.exists(discardedFile)) {
            try {
                Files.delete(discardedFile);
            } catch (
                    IOException e) {
                LOGGER.error("Could not remove discarded file {}", discardedFile, e);
                return true;
            }
            return false;
        }
        return getLatestBackupPath(originalPath, backupDir).map(latestBackupPath -> {
            FileTime latestBackupFileLastModifiedTime;
            try {
                latestBackupFileLastModifiedTime = Files.getLastModifiedTime(latestBackupPath);
            } catch (IOException e) {
                LOGGER.debug("Could not get timestamp of backup file {}", latestBackupPath, e);
                // If we cannot get the timestamp, we do show any warning
                return false;
            }
            FileTime currentFileLastModifiedTime;
            try {
                currentFileLastModifiedTime = Files.getLastModifiedTime(originalPath);
            } catch (IOException e) {
                LOGGER.debug("Could not get timestamp of current file file {}", originalPath, e);
                // If we cannot get the timestamp, we do show any warning
                return false;
            }
            if (latestBackupFileLastModifiedTime.compareTo(currentFileLastModifiedTime) <= 0) {
                // Backup is older than current file
                // We treat the backup as non-different (even if it could differ)
                return false;
            }
            try {
                boolean result = Files.mismatch(originalPath, latestBackupPath) != -1L;
                if (result) {
                    LOGGER.info("Backup file {} differs from current file {}", latestBackupPath, originalPath);
                }
                return result;
            } catch (IOException e) {
                LOGGER.debug("Could not compare original file and backup file.", e);
                // User has to investigate in this case
                return true;
            }
        }).orElse(false);
    }
    /**
     * Restores the backup file by copying and overwriting the original one.
     *
     * @param originalPath Path to the file which should be equalized to the backup file.
     */
    public static void restoreBackup(Path originalPath, Path backupDir) {
        Optional<Path> backupPath = getLatestBackupPath(originalPath, backupDir);
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

}
