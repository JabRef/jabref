package net.sf.jabref.logic.util.date;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import net.sf.jabref.Globals;
import net.sf.jabref.preferences.JabRefPreferences;

public class EasyDateFormat {

    /**
     * The formatter objects
     */
    private DateTimeFormatter dateFormatter;
    private boolean isISOFormat;

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public String getCurrentDate() {
        return getCurrentDate(false);
    }

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public String getCurrentDate(boolean isoFormat) {
        return getDateAt(ZonedDateTime.now(), isoFormat);
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(Date date, boolean isoFormat) {
        return getDateAt(date.toInstant().atZone(ZoneId.systemDefault()), isoFormat);
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(ZonedDateTime dateTime, boolean isoFormat) {
        // first use, create an instance
        if ((dateFormatter == null) || (isoFormat != isISOFormat)) {
            if (isoFormat) {
                dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            } else {
                String format = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT);
                dateFormatter = DateTimeFormatter.ofPattern(format);
            }
            isISOFormat = isoFormat;
        }
        return dateTime.format(dateFormatter);
    }
}
