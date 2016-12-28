package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

/**
     * From BibTex manual:
     * One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
     * (the '+' in this last example indicates pages following that don't form a simple range).
     * To make it easier to maintain Scribe-compatible databases, the standard styles convert
     * a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
     */
public class PagesChecker extends FieldChecker {

    private static final String PAGES_EXP = "" + "\\A" // begin String
            + "\\d+" // number
            + "(?:" // non-capture group
            + "\\+|\\-{2}\\d+" // + or --number (range)
            + ")?" // optional group
            + "(?:" // non-capture group
            + "," // comma
            + "\\d+(?:\\+|\\-{2}\\d+)?" // repeat former pattern
            + ")*" // repeat group 0,*
            + "\\z"; // end String

    private static final Predicate<String> VALID_PAGE_NUMBER = Pattern.compile(PAGES_EXP).asPredicate();

    public PagesChecker() {
        super(FieldName.PAGES);
    }

    /**
     * Checks, if the page numbers String conforms to the BibTex manual
     */
    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (!VALID_PAGE_NUMBER.test(value.trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a valid page number range"), entry, FieldName.PAGES));
        }

        return Collections.emptyList();
    }
}
