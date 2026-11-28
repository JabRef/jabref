package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.logic.help.HelpFile;

/// This Action keeps a reference to a URL. When activated, it shows the help
/// Dialog unless it is already visible, and shows the URL in it.
public class HelpAction extends SimpleCommand {

    private final HelpFile helpPage;
    private final DialogService dialogService;
    private final NativeDesktop nativeDesktop;

    public HelpAction(HelpFile helpPage, DialogService dialogService, NativeDesktop nativeDesktop) {
        this.helpPage = helpPage;
        this.dialogService = dialogService;
        this.nativeDesktop = nativeDesktop;
    }

    void openHelpPage(HelpFile helpPage) {
        nativeDesktop.openBrowserShowPopup("https://docs.jabref.org/" + helpPage.getPageName(), dialogService);
    }

    @Override
    public void execute() {
        openHelpPage(helpPage);
    }
}
