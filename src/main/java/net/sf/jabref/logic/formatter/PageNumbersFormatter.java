package net.sf.jabref.logic.formatter;

import net.sf.jabref.model.entry.BibtexEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class includes sensible defaults for consistent formatting of BibTex page numbers.
 */
public class PageNumbersFormatter {
    private BibtexEntry entry;

    public PageNumbersFormatter(BibtexEntry entry) {
        this.entry = entry;
    }

    /**
     * Format page numbers, separated either by commas or double-hyphens.
     * Converts the range number format of the <code>pages</code> field to page_number--page_number.
     * Removes all literals except [0-9,-].
     * Keeps the existing String if the resulting field does not match the expected Regex.
     *
     * <example>
     *     1-2 -> 1--2
     *     1,2,3 -> 1,2,3
     *     {1}-{2} -> 1--2
     *     Invalid -> Invalid
     * </example>
     */
    public void format() {
        final String field = "pages";
        final String rejectLiterals = "[^0-9,-]";
        final Pattern pagesPattern = Pattern.compile("\\A(\\d+)-{1,2}(\\d+)\\Z");
        final String replace = "$1--$2";

        String value = entry.getField(field);

        // nothing to do
        if (value == null || value.isEmpty()) {
            return;
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
            entry.setField(field, newValue);
        }
    }
}
