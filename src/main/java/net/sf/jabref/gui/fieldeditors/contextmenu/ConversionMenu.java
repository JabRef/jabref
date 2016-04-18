/*  Copyright (C) 2015-2016 JabRef contributors.
    Copyright (C) 2015-2016 Oscar Gustafsson.

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

import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

/**
 * @author Oscar Gustafsson
 *
 * Menu to show up on right-click in a text field for converting text formats
 */
public class ConversionMenu extends JMenu {

    public ConversionMenu(JTextComponent opener) {
        super(Localization.lang("Convert"));
        // create menu items, one for each case changer
        for (Formatter converter : BibtexFieldFormatters.CONVERTERS) {
            JMenuItem menuItem = new JMenuItem(converter.getName());
            menuItem.addActionListener(e -> opener.setText(converter.format(opener.getText())));
            this.add(menuItem);
        }
    }
}
