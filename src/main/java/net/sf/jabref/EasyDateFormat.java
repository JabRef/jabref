package net.sf.jabref;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EasyDateFormat {

    /**
     * A static Object for date formatting. Please do not create the object
     * here, because there are some references from the Globals class.....
     */
    static SimpleDateFormat dateFormatter = null;

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public static String easyDateFormat() {
        return easyDateFormat(new Date());
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public static String easyDateFormat(Date date) {
        // first use, create an instance
        if (dateFormatter == null) {
            String format = Globals.prefs.get("timeStampFormat");
            dateFormatter = new SimpleDateFormat(format);
        }
        return dateFormatter.format(date);
    }
}
