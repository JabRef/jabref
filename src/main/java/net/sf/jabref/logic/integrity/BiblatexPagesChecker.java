package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

/**
* Same as {@link PagesChecker} but allows single dash as well
*/
public class BiblatexPagesChecker implements Checker {

    private static final String PAGES_EXP = "" + "\\A" // begin String
            + "\\d+" // number
            + "(?:" // non-capture group
            + "\\+|\\-{1,2}\\d+" // + or --number (range)
            + ")?" // optional group
            + "(?:" // non-capture group
            + "," // comma
            + "\\d+(?:\\+|\\-{1,2}\\d+)?" // repeat former pattern
            + ")*" // repeat group 0,*
            + "\\z"; // end String

    private static final Predicate<String> VALID_PAGE_NUMBER = Pattern.compile(PAGES_EXP).asPredicate();


    /**
     * Checks, if the page numbers String conforms to the BibTex manual
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.PAGES);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if (!VALID_PAGE_NUMBER.test(value.get().trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a valid page number range"), entry, FieldName.PAGES));
        }

        return Collections.emptyList();
    }
}
