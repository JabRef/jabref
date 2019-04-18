package org.jabref.gui.push;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

public class PushToApplicationSettingsDialog {

    public static void showSettingsDialog(DialogService dialogService, PushToApplicationSettings toApp, int n) {

        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(toApp.getJFXSettingPane(n));

        dialogService.showCustomDialogAndWait(Localization.lang("App settings"), dialogPane, ButtonType.OK, ButtonType.CANCEL).ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                toApp.storeSettings();
            }
        });
    }
}
