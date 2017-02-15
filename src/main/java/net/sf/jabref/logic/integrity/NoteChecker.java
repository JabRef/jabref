package net.sf.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;

public class NoteChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();

    private final BibDatabaseContext bibDatabaseContextEdition;


    public NoteChecker(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * BibLaTeX package documentation (Section 4.9.1):
     * The BibLaTeX package will automatically capitalize the first word when required at the beginning of a sentence.
     * Official BibTeX specification:
     * note: Any additional information that can help the reader. The first word should be capitalized.
     */
    @Override
    public Optional<String> checkValue(String value) {
        //BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Optional.of(Localization.lang("should have the first letter capitalized"));
        }

        return Optional.empty();
    }
}
