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

import net.sf.jabref.JabRef;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.JabRefDesktop;

@SuppressWarnings("serial")
public class ForkMeOnGitHubAction extends AbstractAction {

    public ForkMeOnGitHubAction() {
        super(Localization.menuTitle("Fork me on GitHub"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Opens JabRef's GitHub page"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JabRefDesktop.openBrowser("https://github.com/JabRef/jabref");
        } catch (IOException ex) {
            ex.printStackTrace();
            JabRef.jrf.basePanel().output(Localization.lang("Could not open browser.") + " " + Localization.lang("Please open http://github.com/JabRef/jabref manually."));
        }
    }
}
