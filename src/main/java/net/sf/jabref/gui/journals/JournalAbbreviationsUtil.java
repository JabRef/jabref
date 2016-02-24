/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.journals;

import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.l10n.Localization;

import java.util.Collection;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class JournalAbbreviationsUtil {

    public static TableModel getTableModel(Collection<Abbreviation> abbreviations) {
        Object[][] cells = new Object[abbreviations.size()][2];
        int row = 0;
        for (Abbreviation abbreviation : abbreviations) {
            cells[row][0] = abbreviation.getName();
            cells[row][1] = abbreviation.getIsoAbbreviation();
            row++;
        }

        return new DefaultTableModel(cells, new Object[] {Localization.lang("Full name"),
                Localization.lang("Abbreviation")}) {

            @Override
            public boolean isCellEditable(int row1, int column) {
                return false;
            }
        };
    }
}
