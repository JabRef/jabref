package org.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class EditionChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();
    private static final Predicate<String> ONLY_NUMERALS_OR_LITERALS = Pattern.compile("^([0-9]+|[^0-9].+)$")
            .asPredicate();
    private static final Predicate<String> ONLY_NUMERALS = Pattern.compile("[0-9]+").asPredicate();
    private static final String FIRST_EDITION = "1";

    private final BibDatabaseContext bibDatabaseContextEdition;
    private final boolean allowIntegerEdition;


    public EditionChecker(BibDatabaseContext bibDatabaseContext, boolean allowIntegerEdition) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
        this.allowIntegerEdition = allowIntegerEdition;
    }

    /**
     * Checks, if field contains only an integer or a literal (biblatex mode)
     * Checks, if the first letter is capitalized (BibTeX mode)
     * biblatex package documentation:
     * The edition of a printed publication. This must be an integer, not an ordinal.
     * It is also possible to give the edition as a literal string, for example "Third, revised and expanded edition".
     * Official BibTeX specification:
     * The edition of a book-for example, "Second".
     * This should be an ordinal, and should have the first letter capitalized.
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (value.equals(FIRST_EDITION)) {
            return Optional.of(Localization.lang("edition of book reported as just 1"));
        }

        //biblatex
        if (bibDatabaseContextEdition.isBiblatexMode() && !ONLY_NUMERALS_OR_LITERALS.test(value.trim())) {
            return Optional.of(Localization.lang("should contain an integer or a literal"));
        }

        //BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode()) {
           if(!isFirstCharDigit(value))
            if (!allowIntegerEdition) {
                if (!FIRST_LETTER_CAPITALIZED.test(value.trim())) {
                    return Optional.of(Localization.lang("should have the first letter capitalized"));
                }
            } else {
                if (!ONLY_NUMERALS.test(value.trim()) && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
                    return Optional.of(Localization.lang("should have the first letter capitalized"));
                }
            }
        }

        return Optional.empty();
    }
    boolean isFirstCharDigit (String input){
        char[] array = input.toCharArray();
        return Character.isDigit(array[0]);

    }
}
