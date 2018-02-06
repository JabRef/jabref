package org.jabref.gui.help;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class AboutDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog aboutDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("About JabRef"));
        aboutDialog.setDialogPane((DialogPane) this.getView());
        aboutDialog.show();
    }

}
