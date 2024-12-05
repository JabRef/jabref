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
import org.jabref.gui.backup.BackupEntry;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
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
                originalPath);

        return actionOpt.flatMap(action -> {
            try {

                List<RevCommit> commits = BackupManagerGit.retrieveCommits(originalPath, preferences.getFilePreferences().getBackupDirectory(), -1);
                List<BackupEntry> backups = BackupManagerGit.retrieveCommitDetails(commits, originalPath, preferences.getFilePreferences().getBackupDirectory()).reversed();

                if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                    ObjectId commitId = backups.getFirst().getId();

                    BackupManagerGit.restoreBackup(originalPath, preferences.getFilePreferences().getBackupDirectory(), commitId);

                    return Optional.empty();
                } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                    ObjectId commitId = backups.getFirst().getId();

                    return showReviewBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager, commitId, commitId);
                } else if (action == BackupResolverDialog.COMPARE_OLDER_BACKUP) {
                    var recordBackupChoice = showBackupChoiceDialog(dialogService, preferences, backups);

                    if (recordBackupChoice.isEmpty()) {
                        return Optional.empty();
                    }

                    if (recordBackupChoice.get().action() == BackupChoiceDialog.RESTORE_BACKUP) {
                        LOGGER.warn(recordBackupChoice.get().entry().getSize());
                        ObjectId commitId = recordBackupChoice.get().entry().getId();
                        BackupManagerGit.restoreBackup(originalPath, preferences.getFilePreferences().getBackupDirectory(), commitId);
                        return Optional.empty();
                    }
                    if (recordBackupChoice.get().action() == BackupChoiceDialog.REVIEW_BACKUP) {
                        LOGGER.warn(recordBackupChoice.get().entry().getSize());
                        ObjectId latestCommitId = backups.getFirst().getId();
                        ObjectId commitId = recordBackupChoice.get().entry().getId();
                        return showReviewBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager, commitId, latestCommitId);
                    }
                }
            } catch (GitAPIException | IOException e) {
                throw new RuntimeException(e);
            }
            return Optional.empty();
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService,
                                                                 Path originalPath) {
        return UiTaskExecutor.runInJavaFXThread(
                () -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath)));
    }

    private static Optional<BackupChoiceDialogRecord> showBackupChoiceDialog(DialogService dialogService,
                                                                             GuiPreferences preferences,
                                                                             List<BackupEntry> backups) {
        return UiTaskExecutor.runInJavaFXThread(
                () -> dialogService.showCustomDialogAndWait(new BackupChoiceDialog(preferences.getFilePreferences().getBackupDirectory(), backups)));
    }

    private static Optional<ParserResult> showReviewBackupDialog(
            DialogService dialogService,
            Path originalPath,
            GuiPreferences preferences,
            FileUpdateMonitor fileUpdateMonitor,
            UndoManager undoManager,
            StateManager stateManager,
            ObjectId commitIdToReview,
            ObjectId latestCommitId) {
        try {
            ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();

            // The database of the originalParserResult will be modified
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, fileUpdateMonitor);
            // This will be modified by using the `DatabaseChangesResolverDialog`.
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();

            Path backupPath = preferences.getFilePreferences().getBackupDirectory();

            BackupManagerGit.writeBackupFileToCommit(originalPath, backupPath, commitIdToReview);

            Path backupFilePath = BackupManagerGit.getBackupFilePath(originalPath, backupPath);

            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupFilePath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

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
                BackupManagerGit.writeBackupFileToCommit(originalPath, backupPath, latestCommitId);
                return showRestoreBackupDialog(dialogService, originalPath, preferences, fileUpdateMonitor, undoManager, stateManager);
            });
        } catch (IOException e) {
            LOGGER.error("Error while loading backup or current database", e);
            return Optional.empty();
        }
    }
}
