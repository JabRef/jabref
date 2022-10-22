package org.jabref.gui.dialogs;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

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
                var allChangesResolved = showReviewBackupDialog(dialogService);
                if (allChangesResolved.isEmpty() || !allChangesResolved.get()) {
                    showRestoreBackupDialog(dialogService, originalPath);
                }
            }
        });
    }

    private static Optional<ButtonType> showBackupResolverDialog(DialogService dialogService, Path originalPath) {
        return DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath)));
    }

    private static Optional<Boolean> showReviewBackupDialog(DialogService dialogService) {
        return DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showCustomDialogAndWait(new DatabaseChangesResolverDialog(List.of(new BibTexStringAdd(
                new BibtexString("op", "Nasri"), new BibDatabaseContext(), null)),
                new BibDatabaseContext(), dialogService, Globals.stateManager, Globals.getThemeManager(),
                Globals.prefs, "Review Backup")));
    }
}
