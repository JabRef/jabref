package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores all user dialogs related to {@link BackupManager}.
 */
public class BackupUIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupUIManager.class);

    private BackupUIManager() {
    }

    public static Optional<ParserResult> showRestoreBackupDialog(DialogService dialogService, Path originalPath, PreferencesService preferencesService) {
        var actionOpt = showBackupResolverDialog(dialogService, originalPath);
        return actionOpt.flatMap(action -> {
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                BackupManager.restoreBackup(originalPath);
                return Optional.empty();
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                return showReviewBackupDialog(dialogService, originalPath, preferencesService);
            }
            return Optional.empty();
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService, Path originalPath) {
        return DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath)));
    }

    private static Optional<ParserResult> showReviewBackupDialog(DialogService dialogService, Path originalPath, PreferencesService preferencesService) {
        try {
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();

            // The database of the originalParserResult will be modified
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, Globals.getFileUpdateMonitor());
            // This will be modified by using the `DatabaseChangesResolverDialog`.
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();

            Path backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            DatabaseChangeResolverFactory changeResolverFactory = new DatabaseChangeResolverFactory(dialogService, originalDatabase, preferencesService.getBibEntryPreferences());

            return DefaultTaskExecutor.runInJavaFXThread(() -> {
                List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, changeResolverFactory);
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        changes,
                        originalDatabase, "Review Backup"
                );
                var allChangesResolved = dialogService.showCustomDialogAndWait(reviewBackupDialog);
                if (allChangesResolved.isEmpty() || !allChangesResolved.get()) {
                    // In case not all changes are resolved, start from scratch
                    return showRestoreBackupDialog(dialogService, originalPath, preferencesService);
                }

                // This does NOT return the original ParserResult, but a modified version with all changes accepted or rejected
                return Optional.of(originalParserResult);
            });
        } catch (IOException e) {
            LOGGER.error("Error while loading backup or current database", e);
            return Optional.empty();
        }
    }
}
