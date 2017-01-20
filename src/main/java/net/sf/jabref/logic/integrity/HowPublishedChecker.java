package net.sf.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;

public class HowPublishedChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();

    private final BibDatabaseContext databaseContext;


    public HowPublishedChecker(BibDatabaseContext databaseContext) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
    }

    /**
     * Official BibTeX specification:
     *  HowPublished: How something strange has been published. The first word should be capitalized.
     * BibLaTeX package documentation (Section 4.9.1):
     *  The BibLaTeX package will automatically capitalize the first word when required at the beginning of a sentence.
     */
    @Override
    public Optional<String> checkValue(String value) {
        //BibTeX
        if (!databaseContext.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Optional.of(Localization.lang("should have the first letter capitalized"));
        }

        return Optional.empty();
    }
}
