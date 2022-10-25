package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import org.jabref.gui.FXDialog;
import org.jabref.gui.Globals;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

public class BackupResolverDialog extends FXDialog {
    public static final ButtonType RESTORE_FROM_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public BackupResolverDialog(Path originalPath) {
        super(AlertType.CONFIRMATION, Localization.lang("Backup found"), true);
        setHeaderText(null);
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Path backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();

        setContentText(Localization.lang("""
                        A backup file for '%0' was found at '%1'.
                        This could indicate that JabRef did not shut down cleanly last time the file was used.

                        Do you want to recover the library from the backup file?
                        """, originalPath.getFileName().toString(),
                backupPath.getFileName().toString()));

        getDialogPane().getButtonTypes().setAll(RESTORE_FROM_BACKUP, REVIEW_BACKUP, IGNORE_BACKUP);
        ControlHelper.setAction(REVIEW_BACKUP, getDialogPane(), (e) -> reviewBackup());

        setResultConverter(action -> {
            if (RESTORE_FROM_BACKUP == action) {
                System.out.println("Restore from backup");
               /* BackgroundTask.wrap(() -> {
                                  try {
                                      Thread.sleep(6000);
                                  } catch (
                                          InterruptedException e) {
                                      throw new RuntimeException(e);
                                  }
                              })
                              .executeWith(Globals.TASK_EXECUTOR);*/
                DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> {
                    try {
                        Thread.sleep(8000);
                    } catch (
                            InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

            } else if (IGNORE_BACKUP == action) {
                ignoreBackup();
            } else if (REVIEW_BACKUP == action) {
                reviewBackup();
            }
            return action;
        });
    }

    private void restoreBackup() {
    }

    private void ignoreBackup() {
        System.out.println("Ignore backup");
    }

    private void reviewBackup() {
        System.out.println("Review backup");
    }
}
