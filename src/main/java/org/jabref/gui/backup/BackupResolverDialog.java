package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import org.jabref.gui.FXDialog;
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

        // TODO: Localize this string once the localization consistency tests are updated to support Java 15 text blocks
        setContentText("""
                        A backup file for '%s' was found at '%s'.
                        This could indicate that JabRef did not shut down cleanly last time the file was used.

                        Do you want to recover the library from the backup file?
                        """.formatted(originalPath.getFileName().toString(),
                backupPath.getFileName().toString()));

        getDialogPane().getButtonTypes().setAll(RESTORE_FROM_BACKUP, REVIEW_BACKUP, IGNORE_BACKUP);
    }
}
