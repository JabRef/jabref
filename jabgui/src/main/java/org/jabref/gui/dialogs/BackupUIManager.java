package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Stores all user dialogs related to [BackupManager].
@NullMarked
public class BackupUIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupUIManager.class);
    private static final Lock BACKUP_RESOLVER_LOCK = new ReentrantLock();

    private BackupUIManager() {
    }

    // [impl->req~jabgui.autosaveandbackup.focus-backup-library-tab~1]
    public static Optional<ParserResult> showRestoreBackupDialog(DialogService dialogService,
                                                                 LibraryTabContainer tabContainer,
                                                                 Path originalPath,
                                                                 GuiPreferences preferences,
                                                                 FileUpdateMonitor fileUpdateMonitor,
                                                                 StateManager stateManager) {
        BACKUP_RESOLVER_LOCK.lock();
        try {
            Optional<ButtonType> actionOpt = showBackupResolverDialog(
                    dialogService,
                    tabContainer,
                    preferences.getExternalApplicationsPreferences(),
                    originalPath,
                    preferences.getFilePreferences().getBackupDirectory());
            return actionOpt.flatMap(action -> {
                if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                    BackupManager.RestoreResult result = BackupManager.restoreBackup(originalPath, preferences.getFilePreferences().getBackupDirectory());
                    switch (result) {
                        case BackupManager.RestoreResult.Empty(
                                Path backupPath
                        ) ->
                                dialogService.showErrorDialogAndWait(
                                        Localization.lang("Restore backup"),
                                        Localization.lang("The backup file '%0' is empty and was not restored.", backupPath));
                        case BackupManager.RestoreResult.Failed(
                                Path backupPath,
                                IOException exception
                        ) ->
                                dialogService.showErrorDialogAndWait(
                                        Localization.lang("Restore backup"),
                                        Localization.lang("Could not restore the backup file '%0'.", backupPath),
                                        exception);
                        case BackupManager.RestoreResult.NotFound(
                                Path missingOriginalPath
                        ) ->
                                dialogService.showErrorDialogAndWait(
                                        Localization.lang("Restore backup"),
                                        Localization.lang("No backup file was found for '%0'.", missingOriginalPath));
                        case BackupManager.RestoreResult.Restored _ -> {
                        }
                    }
                    return Optional.empty();
                } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                    return showReviewBackupDialog(dialogService, tabContainer, originalPath, preferences, fileUpdateMonitor, stateManager);
                }
                return Optional.empty();
            });
        } finally {
            BACKUP_RESOLVER_LOCK.unlock();
        }
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService,
                                                                 LibraryTabContainer tabContainer,
                                                                 ExternalApplicationsPreferences externalApplicationsPreferences,
                                                                 Path originalPath,
                                                                 Path backupDir) {
        return UiTaskExecutor.runInJavaFXThread(() -> {
            findLibraryTabForPath(tabContainer, originalPath).ifPresent(tabContainer::showLibraryTab);
            return dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath, backupDir, externalApplicationsPreferences));
        });
    }

    private static Optional<LibraryTab> findLibraryTabForPath(LibraryTabContainer tabContainer, Path originalPath) {
        return tabContainer.getLibraryTabs().stream()
                           .filter(tab -> tab.getBibDatabaseContext().getDatabasePath()
                                             .map(Path::toAbsolutePath)
                                             .equals(Optional.of(originalPath.toAbsolutePath())))
                           .findFirst();
    }

    private static Optional<ParserResult> showReviewBackupDialog(
            DialogService dialogService,
            LibraryTabContainer tabContainer,
            Path originalPath,
            GuiPreferences preferences,
            FileUpdateMonitor fileUpdateMonitor,
            StateManager stateManager) {
        try {
            ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();

            // The database of the originalParserResult will be modified
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, fileUpdateMonitor);
            // This will be modified by using the `DatabaseChangesResolverDialog`.
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();

            Path backupPath = BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, preferences.getFilePreferences().getBackupDirectory()).orElseThrow();
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            DatabaseChangeResolverFactory changeResolverFactory = new DatabaseChangeResolverFactory(dialogService, originalDatabase, preferences, stateManager);

            return UiTaskExecutor.runInJavaFXThread(() -> {
                Optional<LibraryTab> targetTabOpt = findLibraryTabForPath(tabContainer, originalPath);
                targetTabOpt.ifPresent(tabContainer::showLibraryTab);

                List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, changeResolverFactory);
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        changes,
                        originalDatabase, Localization.lang("Review backup")
                );
                Optional<Boolean> allChangesResolved = dialogService.showCustomDialogAndWait(reviewBackupDialog);
                if (allChangesResolved.orElse(false)) {
                    List<DatabaseChange> resolvedChanges = reviewBackupDialog.getResolvedChanges();
                    stateManager.getUndoManager(originalDatabase).addEdit(Localization.lang("Merged external changes"), edit ->
                            resolvedChanges.stream().filter(DatabaseChange::isAccepted).forEach(change -> change.applyChange(edit)));
                    if (reviewBackupDialog.areAllChangesDenied()) {
                        // Here the case of a backup file is handled: If no changes of the backup are merged in, the file stays the same
                        targetTabOpt.ifPresent(LibraryTab::resetChangeMonitor);
                    }

                    // This does NOT return the original ParserResult, but a modified version with all changes accepted or rejected
                    return Optional.of(originalParserResult);
                }

                // In case not all changes are resolved, start from scratch
                return showRestoreBackupDialog(dialogService, tabContainer, originalPath, preferences, fileUpdateMonitor, stateManager);
            });
        } catch (IOException e) {
            LOGGER.error("Error while loading backup or current database", e);
            return Optional.empty();
        }
    }
}
