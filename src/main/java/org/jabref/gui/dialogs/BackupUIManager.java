package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.ExternalApplicationsPreferences;
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

    public static Optional<ParserResult> showRestoreBackupDialog(DialogService dialogService,
                                                                 Path originalPath,
                                                                 PreferencesService preferencesService,
                                                                 FileUpdateMonitor fileUpdateMonitor) {
        var actionOpt = showBackupResolverDialog(
                dialogService,
                preferencesService.getExternalApplicationsPreferences(),
                originalPath,
                preferencesService.getFilePreferences().getBackupDirectory());
        return actionOpt.flatMap(action -> {
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                BackupManager.restoreBackup(originalPath, preferencesService.getFilePreferences().getBackupDirectory());
                return Optional.empty();
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                return showReviewBackupDialog(dialogService, originalPath, preferencesService, fileUpdateMonitor);
            }
            return Optional.empty();
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService,
                                                                 ExternalApplicationsPreferences externalApplicationsPreferences,
                                                                 Path originalPath,
                                                                 Path backupDir) {
        return DefaultTaskExecutor.runInJavaFXThread(
                () -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath, backupDir, externalApplicationsPreferences)));
    }

    private static Optional<ParserResult> showReviewBackupDialog(
            DialogService dialogService,
            Path originalPath,
            PreferencesService preferencesService,
            FileUpdateMonitor fileUpdateMonitor) {
        try {
            ImportFormatPreferences importFormatPreferences = preferencesService.getImportFormatPreferences();

            // The database of the originalParserResult will be modified
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, fileUpdateMonitor);
            // This will be modified by using the `DatabaseChangesResolverDialog`.
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();

            Path backupPath = BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, preferencesService.getFilePreferences().getBackupDirectory()).orElseThrow();
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            DatabaseChangeResolverFactory changeResolverFactory = new DatabaseChangeResolverFactory(dialogService, originalDatabase, preferencesService);

            return DefaultTaskExecutor.runInJavaFXThread(() -> {
                List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, changeResolverFactory);
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        changes,
                        originalDatabase, "Review Backup"
                );
                var allChangesResolved = dialogService.showCustomDialogAndWait(reviewBackupDialog);
                if (allChangesResolved.isEmpty() || !allChangesResolved.get()) {
                    // In case not all changes are resolved, start from scratch
                    return showRestoreBackupDialog(dialogService, originalPath, preferencesService, fileUpdateMonitor);
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
