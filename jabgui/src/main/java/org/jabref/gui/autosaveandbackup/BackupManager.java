package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.TableColumn;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Backups the given bib database file from [BibDatabaseContext] on every [BibDatabaseContextChangedEvent].
/// An intelligent [java.util.concurrent.ExecutorService] with a [java.util.concurrent.BlockingQueue] prevents a high load while making backups and
/// rejects all redundant backup tasks. This class does not manage [org.jabref.logic.util.BackupFileType#SAVE] file.
public class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    private static final int MAXIMUM_BACKUP_FILE_COUNT = 10;

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    @NullMarked
    public sealed interface RestoreResult {
        record Restored() implements RestoreResult {
        }

        record Empty(Path backupPath) implements RestoreResult {
        }

        record Failed(Path backupPath, IOException exception) implements RestoreResult {
        }

        record NotFound(Path originalPath) implements RestoreResult {
        }
    }

    private static final Set<BackupManager> RUNNING_INSTANCES = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CoarseChangeFilter coarseChangeFilter;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;

    // Contains a list of all backup paths
    // During writing, the less recent backup file is deleted
    private final Queue<Path> backupFilesQueue = new LinkedBlockingQueue<>();
    private boolean needsBackup = false;

    BackupManager(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, CoarseChangeFilter coarseChangeFilter, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.coarseChangeFilter = coarseChangeFilter;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;
    }

    /// Determines the most recent backup file name
    static Path getBackupPathForNewBackup(Path originalPath, Path backupDir) {
        return BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalPath, BackupFileType.BACKUP, backupDir);
    }

    /// Determines the most recent existing backup file name
    static Optional<Path> getLatestBackupPath(Path originalPath, Path backupDir) {
        return BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, backupDir);
    }

    /// Starts the BackupManager which is associated with the given [BibDatabaseContext]. As long as no database
    /// file is present in [BibDatabaseContext], the [BackupManager] will do nothing.
    ///
    /// This method is not thread-safe. The caller has to ensure that this method is not called in parallel.
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static BackupManager start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, CoarseChangeFilter coarseChangeFilter, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        BackupManager backupManager = new BackupManager(libraryTab, bibDatabaseContext, coarseChangeFilter, entryTypesManager, preferences);
        backupManager.startBackupTask(preferences.getFilePreferences().getBackupDirectory());
        coarseChangeFilter.registerListener(backupManager);
        RUNNING_INSTANCES.add(backupManager);
        return backupManager;
    }

    /// Marks the backup as discarded at the library which is associated with the given [BibDatabaseContext].
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static void discardBackup(BibDatabaseContext bibDatabaseContext, Path backupDir) {
        RUNNING_INSTANCES.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.discardBackup(backupDir));
    }

    /// Shuts down the BackupManager which is associated with the given [BibDatabaseContext].
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    /// @param backupDir          The path to the backup directory
    /// @param createBackup       True, if a backup should be created
    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        RUNNING_INSTANCES.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdown(backupDir, createBackup));
        RUNNING_INSTANCES.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    /// Checks whether a backup file exists for the given database file. If it exists, it is checked whether it is
    /// newer and different from the original.
    ///
    /// In case a discarded file is present, the method also returns `false`, See also [#discardBackup(Path)].
    ///
    /// @param originalPath Path to the file a backup should be checked for. Example: jabref.bib.
    /// @return `true` if backup file exists AND differs from originalPath. `false` is the
    /// "default" return value in the good case. In case a discarded file exists, `false` is returned, too.
    /// In the case of an exception `true` is returned to ensure that the user checks the output.
    public static boolean backupFileDiffers(Path originalPath, Path backupDir, ImportFormatPreferences importFormatPreferences) {
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
                if (Files.mismatch(originalPath, latestBackupPath) == -1L) {
                    return false;
                }
                if (differsOnlyInModificationDate(originalPath, latestBackupPath, importFormatPreferences)) {
                    LOGGER.info("Backup file {} differs from current file {} only in modification dates", latestBackupPath, originalPath);
                    return false;
                }
                LOGGER.info("Backup file {} differs from current file {}", latestBackupPath, originalPath);
                return true;
            } catch (IOException | IllegalCharsetNameException | UnsupportedCharsetException e) {
                LOGGER.debug("Could not compare original file and backup file.", e);
                // User has to investigate in this case
                return true;
            }
        }).orElse(false);
    }

    /// An edit that was reverted still leaves a new `modificationdate` behind, so the backup written afterwards differs
    /// from the file without containing anything worth restoring. Parsing both files is only done once the bytes are
    /// known to differ, so that the common case of opening a library stays cheap. An invalid `% Encoding:` line makes
    /// the parser throw an unchecked charset exception; the caller treats that like an I/O failure.
    /// [impl->req~jabgui.autosaveandbackup.ignore-modification-date~1]
    private static boolean differsOnlyInModificationDate(Path originalPath, Path backupPath, ImportFormatPreferences importFormatPreferences) throws IOException {
        ParserResult original = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, new DummyFileUpdateMonitor());
        ParserResult backup = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor());
        // Custom entry type definitions live in the parser result, not in the database context
        if (original.isInvalid() || backup.isInvalid() || !original.getEntryTypes().equals(backup.getEntryTypes())) {
            return false;
        }
        return BibDatabaseDiff.compare(original.getDatabaseContext(), backup.getDatabaseContext())
                              .differsOnlyInFields(Set.of(StandardField.MODIFICATIONDATE));
    }

    /// Restores the backup file by copying and overwriting the original one.
    /// [impl->req~jabgui.autosaveandbackup.complete-backup~1]
    ///
    /// @param originalPath Path to the file which should be equalized to the backup file.
    public static RestoreResult restoreBackup(Path originalPath, Path backupDir) {
        Optional<Path> backupPath = getLatestBackupPath(originalPath, backupDir);
        if (backupPath.isEmpty()) {
            LOGGER.error("There is no backup file");
            return new RestoreResult.NotFound(originalPath);
        }
        try {
            if (Files.size(backupPath.get()) == 0) {
                LOGGER.warn("Backup file {} is empty and will not be restored", backupPath.get());
                return new RestoreResult.Empty(backupPath.get());
            }
            Files.copy(backupPath.get(), originalPath, StandardCopyOption.REPLACE_EXISTING);
            return new RestoreResult.Restored();
        } catch (IOException e) {
            LOGGER.error("Error while restoring the backup file.", e);
            return new RestoreResult.Failed(backupPath.get(), e);
        }
    }

    Optional<Path> determineBackupPathForNewBackup(Path backupDir) {
        return bibDatabaseContext.getDatabasePath().map(path -> BackupManager.getBackupPathForNewBackup(path, backupDir));
    }

    /// This method is called as soon as the scheduler says: "Do the backup"
    ///
    /// *SIDE EFFECT: Deletes oldest backup file*
    ///
    /// @param backupPath the full path to the file where the library should be backed up to
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
                                                .map(BibEntry::new)
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
        try (AtomicFileWriter writer = new AtomicFileWriter(backupPath, encoding, false)) {
            BibWriter bibWriter = new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator());
            try {
                new BibDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        preferences.getFieldPreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        entryTypesManager)
                        // we save the clone to prevent the original database (and thus the UI) from being changed
                        .writeDatabase(bibDatabaseContextClone);
                backupFilesQueue.add(backupPath);

                // We wrote the file successfully
                // Thus, we currently do not need any new backup
                this.needsBackup = false;
                // [impl->req~jabgui.autosaveandbackup.complete-backup~1]
            } catch (IOException e) {
                writer.abort();
                throw e;
            }
        } catch (IOException e) {
            LOGGER.error("Error while saving to file {}", backupPath, e);
        }
    }

    private static Path determineDiscardedFile(Path file, Path backupDir) {
        return backupDir.resolve(BackupFileUtil.getUniqueFilePrefix(file) + "--" + file.getFileName() + "--discarded");
    }

    /// Marks the backups as discarded.
    ///
    /// We do not delete any files, because the user might want to recover old backup files.
    /// Therefore, we mark discarded backups by a --discarded file.
    public void discardBackup(Path backupDir) {
        Path path = determineDiscardedFile(bibDatabaseContext.getDatabasePath().get(), backupDir);
        try {
            Files.createFile(path);
        } catch (IOException e) {
            LOGGER.info("Could not create backup file {}", path, e);
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
                () -> determineBackupPathForNewBackup(backupDir).ifPresent(this::performBackup),
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    private void fillQueue(Path backupDir) {
        bibDatabaseContext.getDatabasePath().ifPresent(databasePath ->
                backupFilesQueue.addAll(BackupFileUtil.getExistingBackupFiles(databasePath, backupDir)));
    }

    /// Unregisters the BackupManager from the eventBus of [BibDatabaseContext].
    /// This method should only be used when closing a database/JabRef in a normal way.
    ///
    /// @param backupDir    The backup directory
    /// @param createBackup If the backup manager should still perform a backup
    private void shutdown(Path backupDir, boolean createBackup) {
        coarseChangeFilter.unregisterListener(this);
        executor.shutdown();

        if (createBackup) {
            // Ensure that backup is a recent one
            determineBackupPathForNewBackup(backupDir).ifPresent(this::performBackup);
        }
    }
}
