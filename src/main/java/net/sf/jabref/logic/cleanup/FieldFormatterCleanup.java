package net.sf.jabref.logic.cleanup;

import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * This class formats a given entry field with the specified formatter.
 */
public class FieldFormatterCleanup {
    private final String field;
    private final Formatter formatter;

    public static FieldFormatterCleanup PAGE_NUMBERS = new FieldFormatterCleanup("pages",
            BibtexFieldFormatters.PAGE_NUMBERS);
    public static FieldFormatterCleanup DATES = new FieldFormatterCleanup("date", BibtexFieldFormatters.DATE);
    public static FieldFormatterCleanup TITLE_CASE = new FieldFormatterCleanup("title", new CaseKeeper());
    public static FieldFormatterCleanup TITLE_UNITS = new FieldFormatterCleanup("title", new UnitFormatter());
    public static FieldFormatterCleanup TITLE_LATEX = new FieldFormatterCleanup("title", new LatexFormatter());

    public FieldFormatterCleanup(String field, Formatter formatter) {
        this.field = field;
        this.formatter = formatter;
    }

    /**
     * Cleanup the entry by applying the formatter to the specified field.
     */
    public void cleanup(BibtexEntry entry) {
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            // not set
            return;
        }

        // run formatter
        String newValue = formatter.format(oldValue);

        entry.setField(field, newValue);
    }

    public String getField() {
        return field;
    }
}
