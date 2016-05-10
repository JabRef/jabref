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
package net.sf.jabref.gui.fieldeditors.contextmenu;

import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.Formatters;
import net.sf.jabref.logic.l10n.Localization;

public class CaseChangeMenu extends JMenu {

    public CaseChangeMenu(final JTextComponent parent) {
        super(Localization.lang("Change case"));
        Objects.requireNonNull(parent);

        // create menu items, one for each case changer
        for (final Formatter caseChanger : Formatters.CASE_CHANGERS) {
            JMenuItem menuItem = new JMenuItem(caseChanger.getName());
            menuItem.addActionListener(e -> parent.setText(caseChanger.format(parent.getText())));
            this.add(menuItem);
        }
    }
}
