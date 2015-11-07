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
package net.sf.jabref.importer;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;

/**
 * PostOpenAction that checks whether there are warnings about duplicate BibTeX keys, and
 * if so, offers to start the duplicate resolving process.
 */
public class HandleDuplicateWarnings implements PostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult pr) {
        return pr.hasDuplicateKeys();
    }

    @Override
    public void performAction(BasePanel panel, ParserResult pr) {
        int answer = JOptionPane.showConfirmDialog(null,
                "<html><p>" + Localization.lang("This database contains one or more duplicated BibTeX keys.")
                        + "</p><p>" + Localization.lang("Do you want to resolve duplicate keys now?"),
                Localization.lang("Duplicate BibTeX key"), JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            panel.runCommand(Actions.RESOLVE_DUPLICATE_KEYS);
        }
    }
}
