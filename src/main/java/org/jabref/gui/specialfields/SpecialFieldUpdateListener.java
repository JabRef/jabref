package org.jabref.gui.specialfields;

import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.specialfields.SpecialField;

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
        // only sync if keyword sync is enabled
        if (!Globals.prefs.isKeywordSyncEnabled()) {
            return;
        }

        final BibEntry entry = fieldChangedEvent.getBibEntry();
        final String fieldName = fieldChangedEvent.getFieldName();
        // Source editor cycles through all entries
        // if we immediately updated the fields, the entry editor would detect a subsequent change as a user change
        // and re-fire this event
        // e.g., "keyword = {prio1}, priority = {prio2}" and a change at keyword to prio3 would not succeed.
        SwingUtilities.invokeLater(() -> {
            if (FieldName.KEYWORDS.equals(fieldName)) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, Globals.prefs.getKeywordDelimiter());
            } else if (SpecialField.isSpecialField(fieldName)) {
                SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, Globals.prefs.getKeywordDelimiter());
            }
            SwingUtilities.invokeLater(() -> JabRefGUI.getMainFrame().getCurrentBasePanel().updateEntryEditorIfShowing());
        });
    }

    public static SpecialFieldUpdateListener getInstance() {
        if (SpecialFieldUpdateListener.INSTANCE == null) {
            SpecialFieldUpdateListener.INSTANCE = new SpecialFieldUpdateListener();
        }
        return SpecialFieldUpdateListener.INSTANCE;
    }

}
