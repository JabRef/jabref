package net.sf.jabref.gui.autosave;

import java.nio.file.Path;

import javax.swing.JOptionPane;

import net.sf.jabref.autosave.BackupManager;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Stores all user dialogs related to {@link BackupManager}.
 */
public class BackupUIManager {

    public static void showRestoreBackupDialog(Path originalPath) {
        int answer = JOptionPane.showConfirmDialog(null,
                new StringBuilder()
                    .append(Localization.lang("A backup file for '%0' was found.", originalPath.getFileName().toString()))
                    .append("\n")
                    .append(Localization.lang("This could indicate that JabRef did not shut down cleanly last time the file was used."))
                    .append("\n\n")
                    .append(Localization.lang("Do you want to recover the database from the backup file?")).toString(),
                Localization.lang("Backup found"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (answer == 0) {
            BackupManager.restoreBackup(originalPath);
        }
    }
}
