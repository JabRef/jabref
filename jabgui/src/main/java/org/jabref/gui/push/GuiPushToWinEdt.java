package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.push.PushToWinEdt;

public class GuiPushToWinEdt extends PushToWinEdt implements GuiPushToApplication {

    private final DialogService dialogService;

    public GuiPushToWinEdt(DialogService dialogService, PushToApplicationPreferences preferences) {
        super(dialogService, preferences);
        this.dialogService = dialogService;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_WINEDT;
    }

    @Override
    public void sendErrorNotification(String title, String message) {
        dialogService.showErrorDialogAndWait(title, message);
    }
}
