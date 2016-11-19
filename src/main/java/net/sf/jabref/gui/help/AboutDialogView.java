package net.sf.jabref.gui.help;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class AboutDialogView extends FXMLView {

    public AboutDialogView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXDialog aboutDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("About JabRef"));
        aboutDialog.setDialogPane((DialogPane) this.getView());
        aboutDialog.show();
    }

}
