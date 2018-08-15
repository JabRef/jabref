package org.jabref.gui.cleanup;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public class CleanupDialog extends BaseDialog<CleanupPreset> {

    public CleanupDialog(BibDatabaseContext databaseContext, CleanupPreset initialPreset) {
        setTitle(Localization.lang("Cleanup entries"));
        getDialogPane().setPrefSize(600, 600);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        CleanupPresetPanel presetPanel = new CleanupPresetPanel(databaseContext, initialPreset);
        presetPanel.getScrollPane().setVisible(true);
        ScrollPane scrollPane = presetPanel.getScrollPane();
        JFXPanel scrollPanes = new JFXPanel();
        scrollPanes.setScene(new Scene(scrollPane));
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return presetPanel.getCleanupPreset();
            } else {
                return null;
            }
        });

        ControlHelper.setSwingContent(getDialogPane(), scrollPanes);
    }
}
