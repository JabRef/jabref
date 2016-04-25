/*  Copyright (C) 2015 Oliver Kopp
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.menus.help;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DonateAction extends AbstractAction {

    private static final Log LOGGER = LogFactory.getLog(DonateAction.class);

    private static final String DONATION_LINK = "https://github.com/JabRef/jabref/wiki/Donations";

    public DonateAction() {
        super(Localization.menuTitle("Donate to JabRef"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Donate to JabRef"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.DONATE.getSmallIcon());
        putValue(Action.LARGE_ICON_KEY, IconTheme.JabRefIcon.DONATE.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JabRefDesktop.openBrowser(DONATION_LINK);
        } catch (IOException ex) {
            LOGGER.warn("Could not open browser", ex);
            JabRefGUI.getMainFrame().getCurrentBasePanel().output(Localization.lang("Could not open browser."));
        }
    }
}
