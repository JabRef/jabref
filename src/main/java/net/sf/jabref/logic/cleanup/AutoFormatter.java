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
        applySuperscripts();
    }

    /**
     * Converts ordinal numbers to superscripts, e.g. 1st, 2nd or 3rd.
     * Run the replacement for every available BibTex field.
     * Will replace ordinal numbers even if they are semantically wrong, e.g. 21rd
     *
     * <example>
     *     1st Conf. Cloud Computing -> 1\textsuperscript{st} Conf. Cloud Computing
     * </example>
     */
    public void applySuperscripts() {
        // find possible superscripts on word boundaries
        final Pattern pattern = Pattern.compile("\\b(\\d+)(st|nd|rd|th)\\b", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        // adds superscript tag
        final String replace = "$1\\\\textsuperscript{$2}";

        for(String name: entry.getAllFields()) {
            String value = entry.getField(name);

            // nothing to do
            if (value == null || value.isEmpty()) {
                continue;
            }

            Matcher matcher = pattern.matcher(value);
            // replace globally
            String newValue = matcher.replaceAll(replace);

            // write field
            if(!newValue.equals(value)) {
                entry.setField(name, newValue);
            }
        }
    }
}
