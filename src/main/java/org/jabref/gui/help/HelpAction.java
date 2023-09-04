package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.help.HelpFile;
import org.jabref.preferences.FilePreferences;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends SimpleCommand {

    private final HelpFile helpPage;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;

    public HelpAction(HelpFile helpPage, DialogService dialogService, FilePreferences filePreferences) {
        this.helpPage = helpPage;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
    }

    void openHelpPage(HelpFile helpPage) {
        StringBuilder sb = new StringBuilder("https://docs.jabref.org/");
        sb.append(helpPage.getPageName());
        JabRefDesktop.openBrowserShowPopup(sb.toString(), dialogService, filePreferences);
    }

    @Override
    public void execute() {
        openHelpPage(helpPage);
    }
}
