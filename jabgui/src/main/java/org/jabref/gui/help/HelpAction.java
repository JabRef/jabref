package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.help.HelpFile;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends SimpleCommand {

    private final HelpFile helpPage;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationPreferences;

    public HelpAction(HelpFile helpPage, DialogService dialogService, ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.helpPage = helpPage;
        this.dialogService = dialogService;
        this.externalApplicationPreferences = externalApplicationsPreferences;
    }

    void openHelpPage(HelpFile helpPage) {
        NativeDesktop.openBrowserShowPopup("https://docs.jabref.org/" + helpPage.getPageName(), dialogService, externalApplicationPreferences);
    }

    @Override
    public void execute() {
        openHelpPage(helpPage);
    }
}
