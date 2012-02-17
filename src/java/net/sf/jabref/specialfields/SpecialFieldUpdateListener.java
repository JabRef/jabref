/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.specialfields;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import javax.swing.SwingUtilities;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.undo.NamedCompound;

/**
 * Listener triggering 
 *  * an update of keywords if special field has been updated
 *  * an update of special fields if keywords have been updated 
 */
public class SpecialFieldUpdateListener implements VetoableChangeListener {
	
	private static SpecialFieldUpdateListener INSTANCE = null;
	
	// vetoableChange is also called if we call SpecialFieldsUtils.importKeywords
	// therefore, we have to avoid cyclic calls...
	private static boolean noUpdate = false; 

	public void vetoableChange(PropertyChangeEvent e)
			throws PropertyVetoException {
		if (noUpdate) return;
		final BibtexEntry entry = (BibtexEntry) e.getSource();
		final String fieldName = e.getPropertyName();
		// Source editor cycles through all entries
		// if we immediately updated the fields, the entry editor would detect a subsequent change as a user change 
		// and re-fire this event
		// e.g., "keyword = {prio1}, priority = {prio2}" and a change at keyword to prio3 would not succeed. 
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
				// Generating an instance of a NamedCompound and adding it as edit is a quick hack. 
				// The current infrastructure does not foresee to pass a named component to the vetoable change
				// A "good" infrastructure would offer update listeners after calling the VetoableChangeListeners
				// The parameters for them would offer a named component where the methods could add edits.
				if (fieldName.equals("keywords")) {
					NamedCompound nc = new NamedCompound(Globals.lang("Synchronized special fields based on keywords"));
					noUpdate = true;
					SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
					JabRef.jrf.basePanel().undoManager.addEdit(nc);
		            SwingUtilities.invokeLater(new Runnable() {
		                public void run() {
					    	JabRef.jrf.basePanel().updateEntryEditorIfShowing();
					    	noUpdate = false;
		                }
		            });
				} else {
					SpecialField field = SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName(fieldName);
					if (field != null) {
						NamedCompound nc = new NamedCompound(Globals.lang("Synchronized keywords from special fields"));
						noUpdate = true;
						SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, nc);
						JabRef.jrf.basePanel().undoManager.addEdit(nc);
			            SwingUtilities.invokeLater(new Runnable() {
			                public void run() {
						    	JabRef.jrf.basePanel().updateEntryEditorIfShowing();
						    	noUpdate = false;
			                }
			            });
					}
				}
			}
		});
	}
	
	public static SpecialFieldUpdateListener getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SpecialFieldUpdateListener();
		};
		return INSTANCE;
	}

}
