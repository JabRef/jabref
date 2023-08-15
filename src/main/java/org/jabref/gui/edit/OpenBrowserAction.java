package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.preferences.FilePreferences;

public class OpenBrowserAction extends SimpleCommand {

    private final String urlToOpen;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;

    public OpenBrowserAction(String urlToOpen, DialogService dialogService, FilePreferences filePreferences) {
        this.urlToOpen = urlToOpen;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
    }

    @Override
    public void execute() {
        JabRefDesktop.openBrowserShowPopup(urlToOpen, dialogService, filePreferences);
    }
}
