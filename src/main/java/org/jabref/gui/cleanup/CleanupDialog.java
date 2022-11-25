package org.jabref.gui.cleanup;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.CleanupPreferences;
import org.jabref.preferences.FilePreferences;

public class CleanupDialog extends BaseDialog<CleanupPreferences> {
    public CleanupDialog(BibDatabaseContext databaseContext, CleanupPreferences initialPreset, FilePreferences filePreferences) {
        setTitle(Localization.lang("Cleanup entries"));
        getDialogPane().setPrefSize(600, 650);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        CleanupPresetPanel presetPanel = new CleanupPresetPanel(databaseContext, initialPreset, filePreferences);

        // placing the content of the presetPanel in a scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(presetPanel);

        getDialogPane().setContent(scrollPane);
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return presetPanel.getCleanupPreset();
            } else {
                return null;
            }
        });
    }
}
