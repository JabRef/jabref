/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.external;

import javax.swing.JOptionPane;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;

/**
 * Action for upgrading old-style (pre 2.3) PS/PDF links to the new "file" field.
 */
public class UpgradeExternalLinks extends BaseAction {

    private BasePanel panel;

    public UpgradeExternalLinks(BasePanel panel) {

        this.panel = panel;
    }

    public void action() throws Throwable {

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                Globals.lang("This will move all external links from the 'pdf' and 'ps' fields "
                    +"into the '%0' field. Proceed?", GUIGlobals.FILE_FIELD), Globals.lang("Upgrade external links"),
                JOptionPane.YES_NO_OPTION);
        if (answer !=  JOptionPane.YES_OPTION)
            return;
        NamedCompound ce = Util.upgradePdfPsToFile(panel.database(), new String[] {"pdf", "ps"});
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
        panel.output(Globals.lang("Upgraded links."));
    }
}
