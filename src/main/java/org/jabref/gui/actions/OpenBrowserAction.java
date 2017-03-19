package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.jabref.gui.desktop.JabRefDesktop;

public class OpenBrowserAction extends AbstractAction {

    private final String urlToOpen;

    /**
     * OpenBrowserAction without icons - only to be used for menus
     *
     * @param urlToOpen URL string of an URL to be shown in the default web browser
     * @param menuTitle title of the menu entry; should already be localized
     * @param description description shown in a tooltip hovering over the menu/icon bar entry; should already be localized
     */
    public OpenBrowserAction(String urlToOpen, String menuTitle, String description) {
        super(menuTitle);
        this.urlToOpen = urlToOpen;
        putValue(Action.SHORT_DESCRIPTION, description);
    }

    /**
     * OpenBrowserAction with icons
     *
     * @param urlToOpen URL string of an URL to be shown in the default web browser
     * @param menuTitle title of the menu entry; should already be localized
     * @param description description shown in a tooltip hovering over the menu/icon bar entry; should already be localized
     * @param smallIcon smallIcon to be shown in the menus
     * @param largeIcon larger icon to be shown in the icon bar
     */
    public OpenBrowserAction(String urlToOpen, String menuTitle, String description, Icon smallIcon, Icon largeIcon) {
        super(menuTitle, smallIcon);
        this.urlToOpen = urlToOpen;
        putValue(Action.SHORT_DESCRIPTION, description);
        putValue(Action.LARGE_ICON_KEY, largeIcon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JabRefDesktop.openBrowserShowPopup(urlToOpen);
    }

}
