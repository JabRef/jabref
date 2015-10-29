package net.sf.jabref.logic.cleanup;

import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * This class includes sensible defaults for consistent formatting of BibTex page numbers.
 */
public class PageNumbersCleanup {
    private BibtexEntry entry;

    public PageNumbersCleanup(BibtexEntry entry) {
        this.entry = entry;
    }

    /**
     * Format page numbers, separated either by commas or double-hyphens.
     * Converts the range number format of the <code>pages</code> field to page_number--page_number.
     *
     * @see{PageNumbersFormatter}
     */
    public void cleanup() {
        final String field = "pages";

        String value = entry.getField(field);
        String newValue = BibtexFieldFormatters.PAGE_NUMBERS.format(value);
        entry.setField(field, newValue);
    }
}
