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
import org.jabref.logic.importer.ParserResult;
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

    public static Optional<ParserResult> showRestoreBackupDialog(DialogService dialogService, Path originalPath) {
        var actionOpt = showBackupResolverDialog(dialogService, originalPath);
        return actionOpt.flatMap(action -> {
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                BackupManager.restoreBackup(originalPath);
                return Optional.empty();
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                return showReviewBackupDialog(dialogService, originalPath);
            }
            return Optional.empty();
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService, Path originalPath) {
        return DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath)));
    }

    private static Optional<ParserResult> showReviewBackupDialog(DialogService dialogService, Path originalPath) {
        try {
            Path backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
            ParserResult originalParserResult = OpenDatabase.loadDatabase(originalPath, importFormatPreferences, new DummyFileUpdateMonitor());
            BibDatabaseContext originalDatabase = originalParserResult.getDatabaseContext();
            BibDatabaseContext backupDatabase = OpenDatabase.loadDatabase(backupPath, importFormatPreferences, new DummyFileUpdateMonitor()).getDatabaseContext();

            return DefaultTaskExecutor.runInJavaFXThread(() -> {
                DatabaseChangesResolverDialog reviewBackupDialog = new DatabaseChangesResolverDialog(
                        DatabaseChangeList.compareAndGetChanges(originalDatabase, backupDatabase, null),
                        originalDatabase, dialogService, Globals.stateManager, Globals.getThemeManager(), Globals.prefs, "Review Backup"
                );
                var allChangesResolved = dialogService.showCustomDialogAndWait(reviewBackupDialog);
                if (allChangesResolved.isEmpty() || !allChangesResolved.get()) {
                    showRestoreBackupDialog(dialogService, originalPath);
                }
                return Optional.of(originalParserResult);
            });
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
