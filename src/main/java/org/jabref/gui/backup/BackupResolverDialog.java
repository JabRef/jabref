package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class BackupResolverDialog extends FXDialog {
    public static final ButtonType RESTORE_FROM_BACKUP = new ButtonType(Localization.lang("Restore from latest backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review latest backup"), ButtonBar.ButtonData.LEFT);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);
    public static final ButtonType COMPARE_OLDER_BACKUP = new ButtonType("Compare older backup", ButtonBar.ButtonData.LEFT);

    public BackupResolverDialog(Path originalPath) {
        super(AlertType.CONFIRMATION, Localization.lang("Backup found"), true);
        setHeaderText(null);
        getDialogPane().setMinHeight(180);
        getDialogPane().getButtonTypes().setAll(RESTORE_FROM_BACKUP, REVIEW_BACKUP, IGNORE_BACKUP, COMPARE_OLDER_BACKUP);

        String content = Localization.lang("A backup for '%0' was found.", originalPath.getFileName().toString()) + "\n" +
                Localization.lang("This could indicate that JabRef did not shut down cleanly last time the file was used.") + "\n\n" +
                Localization.lang("Do you want to recover the library from the backup file?");
        setContentText(content);
    }
}
