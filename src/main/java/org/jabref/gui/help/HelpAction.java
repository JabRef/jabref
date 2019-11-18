package org.jabref.gui.help;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.help.HelpFile;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends SimpleCommand {

    private HelpFile helpPage;

    public HelpAction(HelpFile helpPage) {
        this.helpPage = helpPage;
    }

    public static void openHelpPage(HelpFile helpPage) {
        StringBuilder sb = new StringBuilder("https://docs.jabref.org/");
        sb.append(helpPage.getPageName());
        JabRefDesktop.openBrowserShowPopup(sb.toString());
    }

    public static SimpleCommand getMainHelpPageCommand() {
        return new SimpleCommand() {
            @Override
            public void execute() {
                openHelpPage(HelpFile.CONTENTS);
            }
        };
    }

    @Override
    public void execute() {
        openHelpPage(helpPage);
    }
}
