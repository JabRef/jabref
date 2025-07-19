package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.push.PushToVim;

public class GuiPushToVim extends PushToVim implements GuiPushToApplication {

    private final DialogService dialogService;

    public GuiPushToVim(DialogService dialogService, PushToApplicationPreferences preferences) {
        super(dialogService, preferences);
        this.dialogService = dialogService;
    }

    @Override
    public GuiPushToApplicationSettings getSettings(PushToApplication application, DialogService dialogService, FilePreferences filePreferences, PushToApplicationPreferences preferences) {
        return new GuiPushToVimSettings(application, dialogService, filePreferences, preferences);
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_VIM;
    }

    @Override
    public void sendErrorNotification(String title, String message) {
        dialogService.showErrorDialogAndWait(title, message);
    }
}
