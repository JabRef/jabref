package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

/**
 * Stores all user dialogs related to {@link BackupManager}.
 */
public class BackupUIManager {

    private BackupUIManager() {
    }

    public static void showRestoreBackupDialog(DialogService dialogService, Path originalPath) {
        var actionOpt = showBackupResolverDialog(dialogService, originalPath);
        actionOpt.ifPresent(action -> {
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                BackupManager.restoreBackup(originalPath);
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                var allChangesResolved = showReviewBackupDialog(dialogService, originalPath);
                if (allChangesResolved.isEmpty() || !allChangesResolved.get()) {
                    showRestoreBackupDialog(dialogService, originalPath);
                }
            }
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService, Path originalPath) {
        return DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath)));
    }

    private static Optional<Boolean> showReviewBackupDialog(DialogService dialogService, Path originalPath) {
        try {
            Path backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
            BibDatabaseContext originalDatabase = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            return DefaultTaskExecutor.runInJavaFXThread(() -> {
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, null),
                        originalDatabase, dialogService, Globals.stateManager, Globals.getThemeManager(), Globals.prefs, "Review Backup"
                );
                return dialogService.showCustomDialogAndWait(reviewBackupDialog);
            });
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
