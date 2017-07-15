package org.jabref.gui.filelist;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;

public class FileListDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog filelistDialog = new FXDialog(AlertType.INFORMATION, "FileLIstDialog ");
        filelistDialog.setDialogPane((DialogPane) this.getView());
        filelistDialog.setResizable(true);
        filelistDialog.show();

    }
}
