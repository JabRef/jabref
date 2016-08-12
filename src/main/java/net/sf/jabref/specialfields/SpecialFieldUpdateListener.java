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

import javax.swing.SwingUtilities;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Listener triggering
 *  * an update of keywords if special field has been updated
 *  * an update of special fields if keywords have been updated
 */
public class SpecialFieldUpdateListener {

    private static SpecialFieldUpdateListener INSTANCE;

    @Subscribe
    public void listen(FieldChangedEvent fieldChangedEvent) {
        final BibEntry entry = fieldChangedEvent.getBibEntry();
        final String fieldName = fieldChangedEvent.getFieldName();
        // Source editor cycles through all entries
        // if we immediately updated the fields, the entry editor would detect a subsequent change as a user change
        // and re-fire this event
        // e.g., "keyword = {prio1}, priority = {prio2}" and a change at keyword to prio3 would not succeed.
        SwingUtilities.invokeLater(() -> {
            if (FieldName.KEYWORDS.equals(fieldName)) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry);
                SwingUtilities
                        .invokeLater(() -> JabRefGUI.getMainFrame().getCurrentBasePanel().updateEntryEditorIfShowing());
            } else {
                if (SpecialFieldsUtils.isSpecialField(fieldName)) {
                    SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry);
                    SwingUtilities.invokeLater(
                            () -> JabRefGUI.getMainFrame().getCurrentBasePanel().updateEntryEditorIfShowing());
                }
            }
        });
    }

    public static SpecialFieldUpdateListener getInstance() {
        if (SpecialFieldUpdateListener.INSTANCE == null) {
            SpecialFieldUpdateListener.INSTANCE = new SpecialFieldUpdateListener();
        }
        return SpecialFieldUpdateListener.INSTANCE;
    }

}
