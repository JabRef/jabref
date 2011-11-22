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
package net.sf.jabref.imports;

import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JOptionPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;

/**
 * This action checks whether any new custom entry types were loaded from this
 * bib file. If so, an offer to remember these entry types is given.
 */
public class CheckForNewEntryTypesAction implements PostOpenAction {

    public boolean isActionNecessary(ParserResult pr) {
        // See if any custom entry types were imported, but disregard those we already know:
        for (Iterator<String> i = pr.getEntryTypes().keySet().iterator(); i.hasNext();) {
            String typeName = (i.next()).toLowerCase();
            if (BibtexEntryType.ALL_TYPES.get(typeName) != null)
                i.remove();
        }
        return pr.getEntryTypes().size() > 0;
    }

    public void performAction(BasePanel panel, ParserResult pr) {

        StringBuffer sb = new StringBuffer(Globals.lang("Custom entry types found in file") + ": ");
        Object[] types = pr.getEntryTypes().keySet().toArray();
        Arrays.sort(types);
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].toString()).append(", ");
        }
        String s = sb.toString();
        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                s.substring(0, s.length() - 2) + ".\n"
                        + Globals.lang("Remember these entry types?"),
                Globals.lang("Custom entry types"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (answer == JOptionPane.YES_OPTION) {
            // Import
            for (BibtexEntryType typ : pr.getEntryTypes().values()){
                BibtexEntryType.ALL_TYPES.put(typ.getName().toLowerCase(), typ);
            }

        }
    }

}
