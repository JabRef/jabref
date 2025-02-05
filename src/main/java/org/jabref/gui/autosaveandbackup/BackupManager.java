package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.TableColumn;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

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

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static final Set<BackupManager> RUNNING_INSTANCES = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;

    // Contains a list of all backup paths
    // During writing, the less recent backup file is deleted
    private final Queue<Path> backupFilesQueue = new LinkedBlockingQueue<>();
    private boolean needsBackup = false;

    BackupManager(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);
    }

    /**
     * Determines the most recent backup file name
     */
    static Path getBackupPathForNewBackup(Path originalPath, Path backupDir) {
        return BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalPath, BackupFileType.BACKUP, backupDir);
    }

    /**
     * Determines the most recent existing backup file name
     */
    static Optional<Path> getLatestBackupPath(Path originalPath, Path backupDir) {
        return BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, backupDir);
    }

    /**
     * Starts the BackupManager which is associated with the given {@link BibDatabaseContext}. As long as no database
     * file is present in {@link BibDatabaseContext}, the {@link BackupManager} will do nothing.
     *
     * This method is not thread-safe. The caller has to ensure that this method is not called in parallel.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static BackupManager start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        BackupManager backupManager = new BackupManager(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        backupManager.startBackupTask(preferences.getFilePreferences().getBackupDirectory());
        RUNNING_INSTANCES.add(backupManager);
        return backupManager;
    }

    /**
     * Marks the backup as discarded at the library which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static void discardBackup(BibDatabaseContext bibDatabaseContext, Path backupDir) {
        RUNNING_INSTANCES.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.discardBackup(backupDir));
    }

    /**
     * Shuts down the BackupManager which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     * @param createBackup True, if a backup should be created
     * @param backupDir The path to the backup directory
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        RUNNING_INSTANCES.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdown(backupDir, createBackup));
        RUNNING_INSTANCES.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
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
    public static boolean backupFileDiffers(Path originalPath, Path backupDir) {
        Path discardedFile = determineDiscardedFile(originalPath, backupDir);
        if (Files.exists(discardedFile)) {
            try {
                Files.delete(discardedFile);
            } catch (IOException e) {
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

    Optional<Path> determineBackupPathForNewBackup(Path backupDir) {
        return bibDatabaseContext.getDatabasePath().map(path -> BackupManager.getBackupPathForNewBackup(path, backupDir));
    }

    /**
     * This method is called as soon as the scheduler says: "Do the backup"
     *
     * <em>SIDE EFFECT: Deletes oldest backup file</em>
     *
     * @param backupPath the full path to the file where the library should be backed up to
     */
    void performBackup(Path backupPath) {
        if (!needsBackup) {
            return;
        }

        // We opted for "while" to delete backups in case there are more than 10
        while (backupFilesQueue.size() >= MAXIMUM_BACKUP_FILE_COUNT) {
            Path oldestBackupFile = backupFilesQueue.poll();
            try {
                Files.delete(oldestBackupFile);
            } catch (IOException e) {
                LOGGER.error("Could not delete backup file {}", oldestBackupFile, e);
            }
        }

        // code similar to org.jabref.gui.exporter.SaveDatabaseAction.saveDatabase
        SelfContainedSaveOrder saveOrder = bibDatabaseContext
                .getMetaData().getSaveOrder()
                .map(so -> {
                    if (so.getOrderType() == SaveOrder.OrderType.TABLE) {
                        // We need to "flatten out" SaveOrder.OrderType.TABLE as BibWriter does not have access to preferences
                        List<TableColumn<BibEntryTableViewModel, ?>> sortOrder = libraryTab.getMainTable().getSortOrder();
                        return new SelfContainedSaveOrder(
                                SaveOrder.OrderType.SPECIFIED,
                                sortOrder.stream()
                                         .filter(col -> col instanceof MainTableColumn<?>)
                                         .map(column -> ((MainTableColumn<?>) column).getModel())
                                         .flatMap(model -> model.getSortCriteria().stream())
                                         .toList());
                    } else {
                        return SelfContainedSaveOrder.of(so);
                    }
                })
                .orElse(SaveOrder.getDefaultSaveOrder());
        SelfContainedSaveConfiguration saveConfiguration = (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration()
                .withMakeBackup(false)
                .withSaveOrder(saveOrder)
                .withReformatOnSave(preferences.getLibraryPreferences().shouldAlwaysReformatOnSave());

        // "Clone" the database context
        // We "know" that "only" the BibEntries might be changed during writing (see [org.jabref.logic.exporter.BibDatabaseWriter.savePartOfDatabase])
        List<BibEntry> list = bibDatabaseContext.getDatabase().getEntries().stream()
                                                .map(BibEntry::clone)
                                                .map(BibEntry.class::cast)
                                                .toList();
        BibDatabase bibDatabaseClone = new BibDatabase(list);
        bibDatabaseContext.getDatabase().getStringValues().stream().map(BibtexString::clone)
                          .map(BibtexString.class::cast)
                          .forEach(bibDatabaseClone::addString);
        BibDatabaseContext bibDatabaseContextClone = new BibDatabaseContext(bibDatabaseClone, bibDatabaseContext.getMetaData());

        Charset encoding = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);
        // We want to have successful backups only
        // Thus, we do not use a plain "FileWriter", but the "AtomicFileWriter"
        // Example: What happens if one hard powers off the machine (or kills the jabref process) during writing of the backup?
        //          This MUST NOT create a broken backup file that then jabref wants to "restore" from?
        try (Writer writer = new AtomicFileWriter(backupPath, encoding, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    preferences.getFieldPreferences(),
                    preferences.getCitationKeyPatternPreferences(),
                    entryTypesManager)
                    // we save the clone to prevent the original database (and thus the UI) from being changed
                    .saveDatabase(bibDatabaseContextClone);
            backupFilesQueue.add(backupPath);

            // We wrote the file successfully
            // Thus, we currently do not need any new backup
            this.needsBackup = false;
        } catch (IOException e) {
            logIfCritical(backupPath, e);
        }
    }

    private static Path determineDiscardedFile(Path file, Path backupDir) {
        return backupDir.resolve(BackupFileUtil.getUniqueFilePrefix(file) + "--" + file.getFileName() + "--discarded");
    }

    /**
     * Marks the backups as discarded.
     *
     * We do not delete any files, because the user might want to recover old backup files.
     * Therefore, we mark discarded backups by a --discarded file.
     */
    public void discardBackup(Path backupDir) {
        Path path = determineDiscardedFile(bibDatabaseContext.getDatabasePath().get(), backupDir);
        try {
            Files.createFile(path);
        } catch (IOException e) {
            LOGGER.info("Could not create backup file {}", path, e);
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
            this.needsBackup = true;
        }
    }

    private void startBackupTask(Path backupDir) {
        fillQueue(backupDir);

        executor.scheduleAtFixedRate(
                                     // We need to determine the backup path on each action, because we use the timestamp in the filename
                                     () -> determineBackupPathForNewBackup(backupDir).ifPresent(path -> this.performBackup(path)),
                                     DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                                     DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                                     TimeUnit.SECONDS);
    }

    private void fillQueue(Path backupDir) {
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
     * Unregisters the BackupManager from the eventBus of {@link BibDatabaseContext}.
     * This method should only be used when closing a database/JabRef in a normal way.
     *
     * @param backupDir The backup directory
     * @param createBackup If the backup manager should still perform a backup
     */
    private void shutdown(Path backupDir, boolean createBackup) {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        executor.shutdown();

        if (createBackup) {
            // Ensure that backup is a recent one
            determineBackupPathForNewBackup(backupDir).ifPresent(this::performBackup);
        }
    }
}
