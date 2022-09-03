package org.jabref.gui.cleanup;

import javafx.scene.control.ButtonType;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

public class CleanupDialog extends BaseDialog<CleanupPreferences> {

    public CleanupDialog(BibDatabaseContext databaseContext, CleanupPreferences initialPreset, FilePreferences filePreferences) {
        setTitle(Localization.lang("Cleanup entries"));
        getDialogPane().setPrefSize(600, 600);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        CleanupPresetPanel presetPanel = new CleanupPresetPanel(databaseContext, initialPreset, filePreferences);
        getDialogPane().setContent(presetPanel);
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return presetPanel.getCleanupPreset();
            } else {
                return null;
            }
        });
    }
}
