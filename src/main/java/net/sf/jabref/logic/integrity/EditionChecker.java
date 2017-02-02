package net.sf.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;

public class EditionChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();
    private static final Predicate<String> ONLY_NUMERALS_OR_LITERALS = Pattern.compile("^([0-9]+|[^0-9].+)$")
            .asPredicate();

    private final BibDatabaseContext bibDatabaseContextEdition;


    public EditionChecker(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * Checks, if field contains only an integer or a literal (BibLaTeX mode)
     * Checks, if the first letter is capitalized (BibTeX mode)
     * BibLaTeX package documentation:
     * The edition of a printed publication. This must be an integer, not an ordinal.
     * It is also possible to give the edition as a literal string, for example "Third, revised and expanded edition".
     * Official BibTeX specification:
     * The edition of a book-for example, "Second".
     * This should be an ordinal, and should have the first letter capitalized.
     */
    @Override
    public Optional<String> checkValue(String value) {
        //BibLaTeX
        if (bibDatabaseContextEdition.isBiblatexMode() && !ONLY_NUMERALS_OR_LITERALS.test(value.trim())) {
            return Optional.of(Localization.lang("should contain an integer or a literal"));
        }

        //BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Optional.of(Localization.lang("should have the first letter capitalized"));
        }

        return Optional.empty();
    }
}
