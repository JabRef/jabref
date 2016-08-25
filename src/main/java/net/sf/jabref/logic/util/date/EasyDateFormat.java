package net.sf.jabref.logic.util.date;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class EasyDateFormat {

    /**
     * The formatter objects
     */
    private final DateTimeFormatter dateFormatter;


    public EasyDateFormat(String dateFormat) {
        this(DateTimeFormatter.ofPattern(dateFormat));
    }

    public EasyDateFormat(DateTimeFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public String getCurrentDate() {
        return getDateAt(ZonedDateTime.now());
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(Date date) {
        return getDateAt(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(ZonedDateTime dateTime) {
        // first use, create an instance
        return dateTime.format(dateFormatter);
    }

    public static EasyDateFormat fromTimeStampFormat(String timeStampFormat) {
        return new EasyDateFormat(timeStampFormat);
    }

    public static EasyDateFormat isoDateFormat() {
        return new EasyDateFormat(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
