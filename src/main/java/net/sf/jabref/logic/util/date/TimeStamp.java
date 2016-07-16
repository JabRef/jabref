package net.sf.jabref.logic.util.date;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

public class TimeStamp {

    public static boolean updateTimeStampIsSet(JabRefPreferences prefs) {
        return prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP)
                && prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP);
    }

    /**
     * Updates the timestamp of the given entry and returns the FieldChange
     */
    public static Optional<FieldChange> doUpdateTimeStamp(BibEntry entry, JabRefPreferences prefs) {
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        String timestamp = EasyDateFormat.fromPreferences(prefs).getCurrentDate();
        return UpdateField.updateField(entry, timeStampField, timestamp);
    }


}
