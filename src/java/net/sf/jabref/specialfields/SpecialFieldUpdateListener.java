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
import net.sf.jabref.JabRef;

/**
 * Listener triggering 
 *  * an update of keywords if special field has been updated
 *  * an update of special fields if keywords have been updated 
 */
public class SpecialFieldUpdateListener implements VetoableChangeListener {
	
	private static SpecialFieldUpdateListener INSTANCE = null;
	
	public void vetoableChange(PropertyChangeEvent e)
			throws PropertyVetoException {
		final BibtexEntry entry = (BibtexEntry) e.getSource();
		final String fieldName = e.getPropertyName();
		// Source editor cycles through all entries
		// if we immediately updated the fields, the entry editor would detect a subsequent change as a user change 
		// and re-fire this event
		// e.g., "keyword = {prio1}, priority = {prio2}" and a change at keyword to prio3 would not succeed. 
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
				if (fieldName.equals("keywords")) {
					// we do NOT pass a named component indicating that we do not want to have undo capabilities
					// if the user undoes the change in the keyword field, this method is also called and 
					// the special fields are updated accordingly 
					SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, null);
		            SwingUtilities.invokeLater(new Runnable() {
		                public void run() {
					    	JabRef.jrf.basePanel().updateEntryEditorIfShowing();
		                }
		            });
				} else {
					SpecialField field = SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName(fieldName);
					if (field != null) {
						// we do NOT pass a named component indicating that we do not want to have undo capabilities
						// if the user undoes the change in the sepcial field, this method is also called and 
						// the keyword field is updated accordingly 
						SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, null);
			            SwingUtilities.invokeLater(new Runnable() {
			                public void run() {
						    	JabRef.jrf.basePanel().updateEntryEditorIfShowing();
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
