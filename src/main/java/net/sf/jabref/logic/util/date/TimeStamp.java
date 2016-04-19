package net.sf.jabref.logic.util.date;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.entry.BibEntry;

public class TimeStamp {

    private static final EasyDateFormat DATE_FORMATTER = new EasyDateFormat();

    public static boolean updateTimeStampIsSet() {
        return Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP)
                && Globals.prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP);
    }

    /**
     * Updates the timestamp of the given entry, nests the given undaoableEdit in a named compound, and returns that
     * named compound
     */
    public static Optional<FieldChange> doUpdateTimeStamp(BibEntry entry) {
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        String timestamp = DATE_FORMATTER.getCurrentDate();
        return UpdateField.updateField(entry, timeStampField, timestamp);
    }


}
