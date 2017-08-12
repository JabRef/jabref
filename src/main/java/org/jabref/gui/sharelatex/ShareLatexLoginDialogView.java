package org.jabref.gui.sharelatex;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;

public class ShareLatexLoginDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog sharelatexProjectDialog = new FXDialog(AlertType.INFORMATION, "Sharelatex Project Dialog");
        sharelatexProjectDialog.setDialogPane((DialogPane) this.getView());
        sharelatexProjectDialog.setResizable(true);
        sharelatexProjectDialog.show();
    }

}
