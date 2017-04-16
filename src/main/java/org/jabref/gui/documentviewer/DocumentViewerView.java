package org.jabref.gui.documentviewer;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class DocumentViewerView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog dialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Document viewer"), false);
        DialogPane dialogPane = (DialogPane) this.getView();

        // Remove button bar at bottom
        dialogPane.getChildren().removeIf(node -> node instanceof ButtonBar);

        dialog.setDialogPane(dialogPane);
        dialog.setResizable(true);
        dialog.show();
    }
}
