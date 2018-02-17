package org.jabref.gui.strings;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;

public class StringDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog stringDialog = new FXDialog(AlertType.INFORMATION, "Edit Strings");
        stringDialog.setDialogPane((DialogPane) this.getView());
        stringDialog.setResizable(true);
        stringDialog.show();
    }
}
