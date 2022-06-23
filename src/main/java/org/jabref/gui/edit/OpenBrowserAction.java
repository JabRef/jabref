package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;

public class OpenBrowserAction extends SimpleCommand {

    private final String urlToOpen;
    private final DialogService dialogService;

    public OpenBrowserAction(String urlToOpen, DialogService dialogService) {
        this.urlToOpen = urlToOpen;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        JabRefDesktop.openBrowserShowPopup(urlToOpen, dialogService);
    }
}
