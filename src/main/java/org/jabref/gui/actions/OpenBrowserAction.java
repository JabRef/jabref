package org.jabref.gui.actions;

import org.jabref.gui.desktop.JabRefDesktop;

public class OpenBrowserAction extends SimpleCommand {

    private final String urlToOpen;

    public OpenBrowserAction(String urlToOpen) {
        this.urlToOpen = urlToOpen;
    }

    @Override
    public void execute() {
        JabRefDesktop.openBrowserShowPopup(urlToOpen);

    }

}
