package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.push.PushToLyx;

public class GuiPushToLyx extends PushToLyx implements GuiPushToApplication {

    private final DialogService dialogService;

    public GuiPushToLyx(DialogService dialogService, PushToApplicationPreferences preferences) {
        super(dialogService, preferences);
        this.dialogService = dialogService;
    }

    @Override
    public GuiPushToApplicationSettings getSettings(PushToApplication application, DialogService dialogService, FilePreferences filePreferences, PushToApplicationPreferences pushToApplicationPreferences) {
        return new GuiPushToLyxSettings(application, dialogService, filePreferences, pushToApplicationPreferences);
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_LYX;
    }

    @Override
    public void sendErrorNotification(String title, String message) {
        dialogService.showErrorDialogAndWait(title, message);
    }
}
