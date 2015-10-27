package net.sf.jabref.logic.cleanup;

import net.sf.jabref.model.entry.BibtexEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class includes sensible defaults for consistent formatting of BibTex entries.
 */
public class AutoFormatter {
    private BibtexEntry entry;

    public AutoFormatter(BibtexEntry entry) {
        this.entry = entry;
    }

    /**
     * Runs all default cleanups for the BibTex entry.
     */
    public void runDefaultCleanups() {
        formatPageNumbers();
    }

    /**
     * Format page numbers, separated either by commas or double-hyphens.
     * Converts the range number format of the <code>pages</code> field to page_number--page_number.
     * Keeps the existing String if the field does not match the expected Regex.
     *
     * <example>
     *     1-2 -> 1--2
     *     1,2,3 -> 1,2,3
     *     Invalid -> Invalid
     * </example>
     */
    public void formatPageNumbers() {
        final String field = "pages";
        final Pattern pattern = Pattern.compile("\\A\\s*(\\d+)\\s*-{1,2}\\s*(\\d+)\\s*\\Z");
        final String replace = "$1--$2";

        String value = entry.getField(field);

        // nothing to do
        if (value == null || value.isEmpty()) {
            return;
        }

        Matcher matcher = pattern.matcher(value);
        // replace
        String newValue = matcher.replaceFirst(replace);

        // write field
        entry.setField(field, newValue);
    }
}
