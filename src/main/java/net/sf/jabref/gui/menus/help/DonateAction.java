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

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.JabRefDesktop;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class DonateAction extends AbstractAction {
    private static final String donationLink = "https://www.paypal.com/cgi-bin/webscr?item_name=JabRef+Bibliography+Manager&cmd=_donations&lc=US&currency_code=EUR&business=jabrefmail%40gmail.com";

    public DonateAction() {
        super(Localization.menuTitle("Donate to JabRef"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Donate to JabRef"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.DONATE.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JabRefDesktop.openBrowser(donationLink);
        } catch (IOException ex) {
            ex.printStackTrace();
            JabRef.jrf.basePanel().output(Localization.lang("Could not open browser window."));
        }
    }
}
