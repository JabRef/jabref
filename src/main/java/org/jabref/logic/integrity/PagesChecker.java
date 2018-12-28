package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class PagesChecker implements ValueChecker {

    private static final String PAGES_EXP_BIBTEX = ""
            + "\\A"                 // begin String
            + "("
            + "[A-Za-z]?\\d*"     // optional prefix and number
            + "("
            + "\\+|-{2}"    // separator
            + "[A-Za-z]?\\d*" // optional prefix and number
            + ")?"
            + ",?"                // page range separation
            + ")*"
            + "\\z";                // end String

    private static final String PAGES_EXP_BIBLATEX = ""
            + "\\A"                 // begin String
            + "("
            + "[A-Za-z]?\\d*"     // optional prefix and number
            + "("
            + "\\+|-{1,2}|\u2013" // separator
            + "[A-Za-z]?\\d*" // optional prefix and number
            + ")?"
            + ",?"                // page range separation
            + ")*"
            + "\\z";                // end String

    private final Predicate<String> isValidPageNumber;

    public PagesChecker(BibDatabaseContext databaseContext) {
        if (databaseContext.isBiblatexMode()) {
            isValidPageNumber = Pattern.compile(PAGES_EXP_BIBLATEX).asPredicate();
        } else {
            isValidPageNumber = Pattern.compile(PAGES_EXP_BIBTEX).asPredicate();
        }
    }

    /**
     * From BibTex manual:
     *  One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
     *  (the '+' in this last example indicates pages following that don't form a simple range).
     *  To make it easier to maintain Scribe-compatible databases, the standard styles convert
     *  a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
     * biblatex:
     *  same as above but allows single dash as well
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (!isValidPageNumber.test(value.trim())) {
            return Optional.of(Localization.lang("should contain a valid page number range"));
        }

        return Optional.empty();
    }
}
