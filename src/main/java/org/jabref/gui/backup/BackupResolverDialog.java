package org.jabref.gui.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;

import org.jabref.gui.FXDialog;
import org.jabref.gui.Globals;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

import org.controlsfx.control.HyperlinkLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupResolverDialog extends FXDialog {
    public static final ButtonType RESTORE_FROM_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupResolverDialog.class);

    public BackupResolverDialog(Path originalPath) {
        super(AlertType.CONFIRMATION, Localization.lang("Backup found"), true);
        setHeaderText(null);
        getDialogPane().setMinHeight(180);
        getDialogPane().getButtonTypes().setAll(RESTORE_FROM_BACKUP, REVIEW_BACKUP, IGNORE_BACKUP);

        Optional<Path> backupPathOpt = BackupFileUtil.getPathOfLatestExisingBackupFile(originalPath, BackupFileType.BACKUP);
        String backupFilename = backupPathOpt.map(Path::getFileName).map(Path::toString).orElse(Localization.lang("File not found"));
        String content = new StringBuilder()
                .append(Localization.lang("A backup file for '%0' was found at [%1]",
                        originalPath.getFileName().toString(),
                        backupFilename))
                .append("\n")
                .append(Localization.lang("This could indicate that JabRef did not shut down cleanly last time the file was used."))
                .append("\n\n")
                .append(Localization.lang("Do you want to recover the library from the backup file?"))
                .toString();
        setContentText(content);

        HyperlinkLabel contentLabel = new HyperlinkLabel(content);
        contentLabel.setPrefWidth(360);
        contentLabel.setOnAction((e) -> {
            if (backupPathOpt.isPresent()) {
                if (!(e.getSource() instanceof Hyperlink)) {
                    return;
                }
                String clickedLinkText = ((Hyperlink) (e.getSource())).getText();
                if (backupFilename.equals(clickedLinkText)) {
                    try {
                        JabRefDesktop.openFolderAndSelectFile(backupPathOpt.get(), Globals.prefs, null);
                    } catch (IOException ex) {
                        LOGGER.error("Could not open backup folder", ex);
                    }
                }
            }
        });
        getDialogPane().setContent(contentLabel);
    }
}
