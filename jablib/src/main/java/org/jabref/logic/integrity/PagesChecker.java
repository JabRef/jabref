package org.jabref.logic.integrity;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;

public class PagesChecker implements ValueChecker {

    public static final String ROMAN_NUMBER = "[ivxlcdmIVXLCDM]+";
    public static final String DECIMAL_NUMBER = "\\d+";
    public static final String PAGE_NUMBER = "(" + ROMAN_NUMBER + "|" + DECIMAL_NUMBER + ")";

    public static final String PAGE_AFFIX = "[A-Za-z]?";
    private static final String PAGE = PAGE_AFFIX + PAGE_NUMBER + PAGE_AFFIX;

    private static final String PAGE_CONTINUATION_MODIFIER = "\\+";

    private static final String BIBTEX_SEPARATOR = "(-{2}|\u2013)";
    private static final String PAGE_RANGE_BIBTEX = "("
            + PAGE + BIBTEX_SEPARATOR + PAGE
            + "|"
            + PAGE + PAGE_CONTINUATION_MODIFIER
            + ")";

    private static final String PAGES_EXP_BIBTEX =
            "\\A("
                    + PAGE
                    + "|"
                    + PAGE_RANGE_BIBTEX
                    + ")\\z";

    private static final String SEQUENS_AND_SEQUENTES = "(f{1,2}|sq{1,2})\\.?";
    private static final String BIBLATEX_SEPARATOR = "(-{1,2}|\u2013|/)";  // separator
    private static final String PAGE_RANGE_BIBLATEX = "("
            + PAGE + BIBLATEX_SEPARATOR + PAGE
            + "|"
            + PAGE + "(" + PAGE_CONTINUATION_MODIFIER + "|\\s*" + SEQUENS_AND_SEQUENTES + ")"
            + ")";

    // See https://packages.oth-regensburg.de/ctan/macros/latex/contrib/biblatex/doc/biblatex.pdf#subsubsection.3.15.3 for valid content
    private static final String PAGES_EXP_BIBLATEX =
            "\\A("
                    + PAGE
                    + "|"
                    + PAGE_RANGE_BIBLATEX
                    + ")\\z";

    private static final Predicate<String> IS_VALID_PAGE_NUMBER_BIBTEX = Pattern.compile(PAGES_EXP_BIBTEX).asPredicate();
    private static final Predicate<String> IS_VALID_PAGE_NUMBER_BIBLATEX = Pattern.compile(PAGES_EXP_BIBLATEX).asPredicate();

    private final Predicate<String> isValidPageNumber;

    public PagesChecker(BibDatabaseContext databaseContext) {
        if (databaseContext.isBiblatexMode()) {
            isValidPageNumber = IS_VALID_PAGE_NUMBER_BIBLATEX;
        } else {
            isValidPageNumber = IS_VALID_PAGE_NUMBER_BIBTEX;
        }
    }

    /// From BibTeX manual:
    /// One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
    /// (the '+' in this last example indicates pages following that don't form a simple range).
    /// To make it easier to maintain Scribe-compatible databases, the standard styles convert
    /// a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
    ///
    /// biblatex:
    /// same as above but allows single dash as well
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (Arrays.stream(value.split(","))
                  .map(String::trim)
                  .anyMatch(pageRange -> !isValidPageNumber.test(pageRange))) {
            return Optional.of(Localization.lang("should contain a valid page number range"));
        }
        return Optional.empty();
    }
}
