package org.jabref.gui.copyfiles;

import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert.AlertType;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class CopyFilesDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog copyFilesResultDlg = new FXDialog(AlertType.INFORMATION, Localization.lang("Result"));
        copyFilesResultDlg.setResizable(true);
        copyFilesResultDlg.setDialogPane((DialogPane) this.getView());
        copyFilesResultDlg.show();
    }

}
