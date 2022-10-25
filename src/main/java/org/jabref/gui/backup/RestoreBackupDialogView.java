package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

public class RestoreBackupDialogView extends BaseDialog<Boolean> {
    private final Path originalPath;
    private final Path backupPath;

    public RestoreBackupDialogView(Path originalPath) {
        this.originalPath = originalPath;
        this.backupPath = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP).orElseThrow();

        setResizable(false);
        setGraphic(IconTheme.JabRefIcons.INTEGRITY_INFO.getGraphicNode());

        setTitle(Localization.lang("Backup found"));
        setContentText(Localization.lang("""
                A backup file for '%0' was found at '%1'.
                This could indicate that JabRef did not shut down cleanly last time the file was used.

                Do you want to recover the library from the backup file?
                """, originalPath.getFileName().toString(),
                backupPath.getFileName().toString()));

        ButtonType restoreFromBackup = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
        ButtonType ignoreBackup = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(restoreFromBackup, ignoreBackup);

        setResultConverter(action -> action == restoreFromBackup);
    }
}
