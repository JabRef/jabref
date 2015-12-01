package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * This class transforms date to the format yyyy-mm-dd or yyyy-mm..
 */
public class DateFormatter implements Formatter {
    @Override
    public String getName() {
        return "Date";
    }

    /**
     * Format date string to yyyy-mm-dd or yyyy-mm. Keeps the existing String if it does not match one of the following
     * formats:
     *  "M/y" (covers 9/15, 9/2015, and 09/2015)
     *  "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
     *  "yyyy-MM-dd" (covers 2009-1-15)
     *  "d.M.uuuu" (covers 15.1.2015)
     */
    @Override
    public String format(String value) {
        TemporalAccessor parsedDate = tryParseDate(value);
        if (parsedDate == null) {
            return value;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        return dateFormatter.format(parsedDate);
    }

    /*
     * Try to parse the following formats
     *  "M/y" (covers 9/15, 9/2015, and 09/2015)
     *  "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
     *  "yyyy-MM-dd" (covers 2009-1-15)
     *  "d.M.uuuu" (covers 15.1.2015)
     * The code is essentially taken from http://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat.
     */
    private TemporalAccessor tryParseDate(String dateString) {
        //@formatter:off
        String[] formatStrings = {
                "uuuu-M-d", "uuuu-M",
                "M/uu", "M/uuuu",
                "MMMM d, uuuu", "MMMM, uuuu",
                "d.M.uuuu"};
        //@formatter:on
        for (String formatString : formatStrings) {
            try {
                return DateTimeFormatter.ofPattern(formatString).parse(dateString);
            } catch (DateTimeParseException e) {
            }
        }

        return null;
    }
}
