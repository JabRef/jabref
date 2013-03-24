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
package net.sf.jabref;

import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveEntry;

/**
 *
 * @author  alver
 */
public class StrictDuplicateSearch extends Thread {
    
    BasePanel panel;
    
    /** Creates a new instance of StrictDuplicateSearch */
    public StrictDuplicateSearch(BasePanel bp) {
        this.panel = bp;
        
    }
    
    public void run() {
        HashSet<BibtexEntry> toRemove = new HashSet<BibtexEntry>();
        NamedCompound ce = new NamedCompound(Globals.lang("Remove duplicates"));
        panel.output(Globals.lang("Searching for duplicates..."));
        Object[] keys = panel.database.getKeySet().toArray();
        if ((keys == null) || (keys.length < 2))
            return;
        BibtexEntry[] bes = new BibtexEntry[keys.length];
        for (int i=0; i<keys.length; i++)
            bes[i] = panel.database.getEntryById((String)keys[i]);
        
        for (int i = 0; i<bes.length-1; i++) {
            for (int j = i + 1; j<bes.length; j++) {
                // We only check the entries if none of the two are already marked for removal.
                if (!toRemove.contains(bes[i]) && !toRemove.contains(bes[j]) && 
                    DuplicateCheck.compareEntriesStrictly(bes[i], bes[j]) > 1) {
                    // These two entries are exactly the same, so we can remove one.                    
                    if (!toRemove.contains(bes[i]) && !toRemove.contains(bes[j])) {
                        toRemove.add(bes[j]);
                    }
                }
            }
         }
    
        if (toRemove.size() == 0) {
            panel.output(Globals.lang("No duplicates found")+".");
            return;
        }
               
        // Finished searching. Now, remove all entries scheduled for removal:
        int answer = JOptionPane.showConfirmDialog(panel.frame(), Globals.lang("Duplicates found")+": "+
            toRemove.size()+". "+Globals.lang("Remove all?"), Globals.lang("Remove duplicates"),
            JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.CANCEL_OPTION)
            return;
        
        panel.output(Globals.lang("Duplicates removed")+": "+toRemove.size());
        
        for (Iterator<BibtexEntry> i=toRemove.iterator(); i.hasNext();) {
            BibtexEntry entry = i.next();
            panel.database.removeEntry(entry.getId());        
            ce.addEdit(new UndoableRemoveEntry(panel.database, entry, panel));
        }
     
        ce.end();
        panel.undoManager.addEdit(ce);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {                
                panel.markBaseChanged();
            }
        });

        
    }
}
