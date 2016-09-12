package net.sf.jabref.logic.layout.format;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Inserts the current date (the time a database is being exported).
 *
 * <p>If a fieldText is given, it must be a valid {@link DateTimeFormatter} pattern.
 * If none is given, the format pattern will be <code>yyyy-MM-dd hh:mm:ss z</code>.
 * This follows ISO-8601. Reason: <a href="https://xkcd.com/1179/">https://xkcd.com/1179/</a>.</p>
 *
 * @author andreas_sf at rudert-home dot de
 */
public class CurrentDate implements LayoutFormatter {

    // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd hh:mm:ss z";


    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        String format = CurrentDate.DEFAULT_FORMAT;
        if ((fieldText != null) && !fieldText.trim().isEmpty()) {
            format = fieldText;
        }
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }
}
