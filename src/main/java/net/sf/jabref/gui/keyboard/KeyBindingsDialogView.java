package net.sf.jabref.gui.keyboard;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class KeyBindingsDialogView extends FXMLView {

    public KeyBindingsDialogView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show(KeyBindingPreferences keyBindingPreferences) {
        FXDialog keyBindingsDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Key bindings"));
        keyBindingsDialog.setDialogPane((DialogPane) this.getView());
        KeyBindingsDialogViewModel controller = (KeyBindingsDialogViewModel) fxmlLoader.getController();
        controller.setKeyBindingPreferences(keyBindingPreferences);
        controller.initializeView();
        keyBindingsDialog.setResizable(true);
        ((Stage) keyBindingsDialog.getDialogPane().getScene().getWindow()).setMinHeight(475);
        ((Stage) keyBindingsDialog.getDialogPane().getScene().getWindow()).setMinWidth(375);
        keyBindingsDialog.show();
    }

}
