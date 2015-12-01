package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class includes sensible defaults for consistent formatting of BibTex page numbers.
 *
 * From BibTex manual:
 * One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
 * (the '+' in this last example indicates pages following that don't form a simple range).
 * To make it easier to maintain Scribe-compatible databases, the standard styles convert
 * a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
 */
public class PageNumbersFormatter implements Formatter {
    @Override
    public String getName() {
        return "Page numbers";
    }

    /**
     * Format page numbers, separated either by commas or double-hyphens.
     * Converts the range number format of the <code>pages</code> field to page_number--page_number.
     * Removes all literals except [0-9,-+].
     * Keeps the existing String if the resulting field does not match the expected Regex.
     *
     * <example>
     *     1-2 -> 1--2
     *     1,2,3 -> 1,2,3
     *     {1}-{2} -> 1--2
     *     43+ -> 43+
     *     Invalid -> Invalid
     * </example>
     */
    @Override
    public String format(String value) {
        final String rejectLiterals = "[^0-9,\\-\\+]";
        final Pattern pagesPattern = Pattern.compile("\\A(\\d+)-{1,2}(\\d+)\\Z");
        final String replace = "$1--$2";

        // nothing to do
        if ((value == null) || value.isEmpty()) {
            return value;
        }

        // remove unwanted literals incl. whitespace
        String cleanValue = value.replaceAll(rejectLiterals, "");
        // try to find pages pattern
        Matcher matcher = pagesPattern.matcher(cleanValue);
        // replace
        String newValue = matcher.replaceFirst(replace);
        // replacement?
        if(!newValue.equals(cleanValue)) {
            // write field
            return newValue;
        }
        return value;
    }
}
