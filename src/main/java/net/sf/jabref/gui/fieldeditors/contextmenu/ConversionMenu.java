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

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.Converters;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConversionMenu extends JMenu {

    private final JTextComponent parent;


    public ConversionMenu(JTextComponent opener) {
        super(Localization.lang("Convert"));
        parent = opener;

        // create menu items, one for each case changer
        for (final Converters.Converter converter : Converters.ALL) {
            JMenuItem menuItem = new JMenuItem(Localization.lang(converter.getName()));
            menuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.setText(converter.convert(parent.getText()));
                }
            });
            this.add(menuItem);
        }
    }
}
