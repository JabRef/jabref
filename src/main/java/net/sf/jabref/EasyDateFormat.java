package net.sf.jabref;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EasyDateFormat {

    /**
     * The formatter objects
     */
    private SimpleDateFormat dateFormatter = null;

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public String getCurrentDate() {
        return getDateAt(new Date());
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(Date date) {
        // first use, create an instance
        if (dateFormatter == null) {
            String format = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT);
            dateFormatter = new SimpleDateFormat(format);
        }
        return dateFormatter.format(date);
    }
}
