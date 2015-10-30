package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class transforms ordinal numbers into LaTex superscripts.
 */
public class SuperscriptFormatter implements Formatter {
    @Override
    public String getName() {
        return "Superscripts";
    }

    /**
     * Converts ordinal numbers to superscripts, e.g. 1st, 2nd or 3rd.
     * Will replace ordinal numbers even if they are semantically wrong, e.g. 21rd
     *
     * <example>
     * 1st Conf. Cloud Computing -> 1\textsuperscript{st} Conf. Cloud Computing
     * </example>
     */
    public String format(String value) {
        // find possible superscripts on word boundaries
        final Pattern pattern = Pattern.compile("\\b(\\d+)(st|nd|rd|th)\\b", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        // adds superscript tag
        final String replace = "$1\\\\textsuperscript{$2}";

        // nothing to do
        if (value == null || value.isEmpty()) {
            return value;
        }

        Matcher matcher = pattern.matcher(value);
        // replace globally
        String newValue = matcher.replaceAll(replace);

        return newValue;
    }
}
