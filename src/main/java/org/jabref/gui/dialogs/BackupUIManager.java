package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.autosaveandbackup.BackupManagerGit;
import org.jabref.gui.backup.BackupChoiceDialog;
import org.jabref.gui.backup.BackupChoiceDialogRecord;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores all user dialogs related to {@link BackupManagerGit}.
 */
public class BackupUIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupUIManager.class);

    private BackupUIManager() {
    }

    public static Optional<ParserResult> showRestoreBackupDialog(DialogService dialogService,
                                                                 Path originalPath,
                                                                 GuiPreferences preferences,
                                                                 FileUpdateMonitor fileUpdateMonitor,
                                                                 UndoManager undoManager,
                                                                 StateManager stateManager) {
        LOGGER.info("Show restore backup dialog");
        var actionOpt = showBackupResolverDialog(
                dialogService,
                preferences.getExternalApplicationsPreferences(),
                originalPath,
                preferences.getFilePreferences().getBackupDirectory());
        return actionOpt.flatMap(action -> {
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                try {
                    ObjectId commitId = BackupManagerGit.retrieveCommits(preferences.getFilePreferences().getBackupDirectory(), 1).getFirst().getId();
                    BackupManagerGit.restoreBackup(preferences.getFilePreferences().getBackupDirectory(), commitId);
                } catch (
                        IOException |
                        GitAPIException e
                ) {
                    throw new RuntimeException(e);
                }
                return Optional.empty();
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                return showReviewBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager);
            } else if (action == BackupResolverDialog.COMPARE_OLDER_BACKUP) {
                var recordBackupChoice = showBackupChoiceDialog(dialogService, originalPath, preferences);
                if (recordBackupChoice.isEmpty()) {
                    return Optional.empty();
                }
                if (recordBackupChoice.get().action() == BackupChoiceDialog.RESTORE_BACKUP) {
                    LOGGER.warn(recordBackupChoice.get().entry().getSize());
                    ObjectId commitId = recordBackupChoice.get().entry().getId();
                    BackupManagerGit.restoreBackup(preferences.getFilePreferences().getBackupDirectory(), commitId);
                    return Optional.empty();
                }
                if (recordBackupChoice.get().action() == BackupChoiceDialog.REVIEW_BACKUP) {
                    LOGGER.warn(recordBackupChoice.get().entry().getSize());
                    return showReviewBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager);
                }
            }
            return Optional.empty();
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService,
                                                                 ExternalApplicationsPreferences externalApplicationsPreferences,
                                                                 Path originalPath,
                                                                 Path backupDir) {
        return UiTaskExecutor.runInJavaFXThread(
                () -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath, backupDir, externalApplicationsPreferences)));
    }

    private static Optional<BackupChoiceDialogRecord> showBackupChoiceDialog(DialogService dialogService,
                                                                             Path originalPath,
                                                                             GuiPreferences preferences) {
        return UiTaskExecutor.runInJavaFXThread(
                () -> dialogService.showCustomDialogAndWait(new BackupChoiceDialog(originalPath, preferences.getFilePreferences().getBackupDirectory())));
    }

    private static Optional<ParserResult> showReviewBackupDialog(
            DialogService dialogService,
            Path originalPath,
            GuiPreferences preferences,
            FileUpdateMonitor fileUpdateMonitor,
            UndoManager undoManager,
            StateManager stateManager) {
        try {
            ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();

            // The database of the originalParserResult will be modified
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, fileUpdateMonitor);
            // This will be modified by using the `DatabaseChangesResolverDialog`.
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();

            Path backupPath = BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, preferences.getFilePreferences().getBackupDirectory()).orElseThrow();
            LOGGER.info("Ligne 127, BackupUIManager, Loading backup database from {}", backupPath);
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            DatabaseChangeResolverFactory changeResolverFactory = new DatabaseChangeResolverFactory(dialogService, originalDatabase, preferences);

            return UiTaskExecutor.runInJavaFXThread(() -> {
                List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, changeResolverFactory);
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        changes,
                        originalDatabase, "Review Backup"
                );
                var allChangesResolved = dialogService.showCustomDialogAndWait(reviewBackupDialog);
                LibraryTab saveState = stateManager.activeTabProperty().get().get();
                final NamedCompound CE = new NamedCompound(Localization.lang("Merged external changes"));
                changes.stream().filter(DatabaseChange::isAccepted).forEach(change -> change.applyChange(CE));
                CE.end();
                undoManager.addEdit(CE);
                if (allChangesResolved.get()) {
                    if (reviewBackupDialog.areAllChangesDenied()) {
                        // Here the case of a backup file is handled: If no changes of the backup are merged in, the file stays the same
                        saveState.resetChangeMonitor();
                    } else {
                        // In case any change of the backup is accepted, this means, the in-memory file differs from the file on disk (which is not the backup file)
                        saveState.markBaseChanged();
                    }
                    // This does NOT return the original ParserResult, but a modified version with all changes accepted or rejected
                    return Optional.of(originalParserResult);
                }

                // In case not all changes are resolved, start from scratch
                return showRestoreBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager);
            });
        } catch (IOException e) {
            LOGGER.error("Error while loading backup or current database", e);
            return Optional.empty();
        }
    }
}
