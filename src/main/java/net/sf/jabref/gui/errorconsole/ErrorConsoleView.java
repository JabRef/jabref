package net.sf.jabref.gui.errorconsole;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import net.sf.jabref.gui.AbstractDialogView;
import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

public class ErrorConsoleView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog errorConsole = new FXDialog(AlertType.ERROR, Localization.lang("Event log"), false);
        errorConsole.setDialogPane((DialogPane) this.getView());
        errorConsole.setResizable(true);
        errorConsole.show();
    }
}
