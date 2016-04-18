package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * This class transforms date to the format yyyy-mm-dd or yyyy-mm..
 */
public class NormalizeDateFormatter implements Formatter {
    @Override
    public String getName() {
        return Localization.lang("Normalize date");
    }

    @Override
    public String getKey() {
        return "normalize_date";
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
        Optional<TemporalAccessor> parsedDate = tryParseDate(value);
        if (!parsedDate.isPresent()) {
            return value;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        return dateFormatter.format(parsedDate.get());
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes the date to ISO date format.");
    }

    @Override
    public String getExampleInput() {
        return "29.11.2003";
    }

    /*
     * Try to parse the following formats
     *  "M/y" (covers 9/15, 9/2015, and 09/2015)
     *  "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
     *  "yyyy-MM-dd" (covers 2009-1-15)
     *  "d.M.uuuu" (covers 15.1.2015)
     *  "uuuu.M.d" (covers 2015.1.15)
     * The code is essentially taken from http://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat.
     */
    private Optional<TemporalAccessor> tryParseDate(String dateString) {
        String[] formatStrings = {
                "uuuu-M-d", "uuuu-M",
                "M/uu", "M/uuuu",
                "MMMM d, uuuu", "MMMM, uuuu",
                "d.M.uuuu", "uuuu.M.d"};
        for (String formatString : formatStrings) {
            try {
                return Optional.of(DateTimeFormatter.ofPattern(formatString).parse(dateString));
            } catch (DateTimeParseException ignored) {
                // Ignored
            }
        }

        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return defaultEquals(obj);
    }
}
