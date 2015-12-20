package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.importer.HTMLConverter;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.MonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Formats a given entry field with the specified formatter.
 */
public class FieldFormatterCleanup implements Cleaner {

    private final String field;
    private final Formatter formatter;

    public static Cleaner PAGE_NUMBERS = new FieldFormatterCleanup("pages", BibtexFieldFormatters.PAGE_NUMBERS);
    public static Cleaner DATES = new FieldFormatterCleanup("date", BibtexFieldFormatters.DATE);
    public static Cleaner MONTH = new FieldFormatterCleanup("month", new MonthFormatter());
    public static Cleaner TITLE_CASE = new FieldFormatterCleanup("title", new CaseKeeper());
    public static Cleaner TITLE_UNITS = new FieldFormatterCleanup("title", new UnitFormatter());
    public static Cleaner TITLE_LATEX = new FieldFormatterCleanup("title", new LatexFormatter());
    public static Cleaner TITLE_HTML = new FieldFormatterCleanup("title", new HTMLConverter());


    public FieldFormatterCleanup(String field, Formatter formatter) {
        this.field = field;
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            // Not set -> nothing to do
            return new ArrayList<>();
        }

        // Run formatter
        String newValue = formatter.format(oldValue);

        if (!oldValue.equals(newValue)) {
            entry.setField(field, newValue);
            FieldChange change = new FieldChange(entry, field, oldValue, newValue);
            return Arrays.asList(new FieldChange[] {change});
        } else {
            return new ArrayList<>();
        }
    }
}
