package org.jabref.gui.sharelatex;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;

public class ShareLatexProjectDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog sharelatexProjectDialog = new FXDialog(AlertType.INFORMATION, "Choose Project");
        sharelatexProjectDialog.setDialogPane((DialogPane) this.getView());
        sharelatexProjectDialog.setResizable(true);
        sharelatexProjectDialog.show();
    }

}
