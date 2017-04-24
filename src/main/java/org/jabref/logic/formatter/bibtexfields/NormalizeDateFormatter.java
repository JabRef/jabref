package org.jabref.logic.formatter.bibtexfields;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.Date;

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
        Optional<Date> parsedDate = Date.parse(value);
        return parsedDate.map(Date::getNormalized).orElse(value);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes the date to ISO date format.");
    }

    @Override
    public String getExampleInput() {
        return "29.11.2003";
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
