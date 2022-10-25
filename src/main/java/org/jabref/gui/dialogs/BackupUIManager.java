package org.jabref.gui.dialogs;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.backup.ResolveBackupAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

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

        ButtonType restoreFromBackup = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
        // Problem: reviewing backup should not close the dialog
        ButtonType reviewBackup = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);
        ButtonType ignoreBackup = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);
        int x = DefaultTaskExecutor.runInJavaFXThread(() -> {
            new ResolveBackupAction(originalPath, dialogService).execute();
            return 1;
        });

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
