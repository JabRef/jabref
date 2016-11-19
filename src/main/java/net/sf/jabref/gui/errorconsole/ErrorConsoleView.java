package net.sf.jabref.gui.errorconsole;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class ErrorConsoleView extends FXMLView {

    public ErrorConsoleView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXDialog errorConsole = new FXDialog(AlertType.ERROR, Localization.lang("Event log"), false);
        DialogPane pane = (DialogPane) this.getView();
        errorConsole.setDialogPane(pane);
        errorConsole.setResizable(true);
        errorConsole.show();
    }

}
