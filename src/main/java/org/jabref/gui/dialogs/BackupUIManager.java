package org.jabref.gui.dialogs;

import java.nio.file.Path;
import java.util.List;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.backup.ResolveBackupAction;
import org.jabref.gui.collab.ExternalChangesResolverDialog;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;

/**
 * Stores all user dialogs related to {@link BackupManager}.
 */
public class BackupUIManager {

    private BackupUIManager() {
    }

    @SuppressWarnings("ConstantConditions")
    public static void showRestoreBackupDialog(DialogService dialogService, Path originalPath) {
        Path backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();

        String content = Localization.lang("""
                        A backup file for '%0' was found at '%1'.
                        This could indicate that JabRef did not shut down cleanly last time the file was used.

                        Do you want to recover the library from the backup file?
                        """, originalPath.getFileName().toString(),
                backupPath.getFileName().toString());
        System.out.println("Backup");

       /* ButtonType restoreFromBackup = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
        // Problem: reviewing backup should not close the dialog
        ButtonType reviewBackup = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);
        ButtonType ignoreBackup = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);*/
        var actionOpt = DefaultTaskExecutor.runInJavaFXThread(() -> {
            return dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalPath));
        });

        if (actionOpt.isPresent()) {
            var action = actionOpt.get();
            if (action == BackupResolverDialog.RESTORE_FROM_BACKUP) {
                BackupManager.restoreBackup(originalPath);
            } else if (action == BackupResolverDialog.REVIEW_BACKUP) {
                var wait = dialogService.showCustomDialogAndWait(new ExternalChangesResolverDialog(List.of(),
                        new BibDatabaseContext(), dialogService, Globals.stateManager, Globals.getThemeManager(),
                        Globals.prefs, "Review Backup"));
                System.out.println("Reviewing backup...");
            }
        }

/*        boolean shouldRestoreBackup = DefaultTaskExecutor.runInJavaFXThread(() ->
                dialogService.showCustomButtonDialogAndWait(Alert.AlertType.CONFIRMATION,
                        Localization.lang("Backup found"),
                        content,
                        restoreFromBackup,
                        reviewBackup,
                        ignoreBackup
                ).map(buttonType -> buttonType == ButtonType.APPLY).orElse(false)
        );

        if (shouldRestoreBackup) {
            BackupManager.restoreBackup(originalPath);
        }*/
    }
}
