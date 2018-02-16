package org.jabref.gui.keyboard;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class KeyBindingsDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog keyBindingsDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Key bindings"));
        keyBindingsDialog.setDialogPane((DialogPane) this.getView());
        keyBindingsDialog.setResizable(true);
        ((Stage) keyBindingsDialog.getDialogPane().getScene().getWindow()).setMinHeight(475);
        ((Stage) keyBindingsDialog.getDialogPane().getScene().getWindow()).setMinWidth(375);
        keyBindingsDialog.show();
    }

}
