package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;

public class OpenBrowserAction extends SimpleCommand {

    private final String urlToOpen;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    public OpenBrowserAction(String urlToOpen, DialogService dialogService, ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.urlToOpen = urlToOpen;
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
    }

    @Override
    public void execute() {
        NativeDesktop.openBrowserShowPopup(urlToOpen, dialogService, externalApplicationsPreferences);
    }
}
