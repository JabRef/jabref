package org.jabref.gui.actions;

import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.preftabs.PreferencesDialog;
import org.jabref.logic.l10n.Localization;

public class ShowPreferencesAction extends SimpleCommand {

    private PreferencesDialog prefsDialog;
    private final JabRefFrame jabRefFrame;

    public ShowPreferencesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        if (prefsDialog == null) {
            prefsDialog = new PreferencesDialog(jabRefFrame);
        } else {
            prefsDialog.setValues();
        }

        ButtonType save = new ButtonType(Localization.lang("Save"), ButtonBar.ButtonData.APPLY);
        Optional<ButtonType> response = jabRefFrame.getDialogService().showCustomSwingDialogAndWait(
                Localization.lang("JabRef preferences"),
                prefsDialog.getMainPanel(), 1000, 800,
                save, ButtonType.CANCEL
        );

        if (response.isPresent() && response.get().equals(save)) {
            prefsDialog.storeAllSettings();
        }
    }
}
