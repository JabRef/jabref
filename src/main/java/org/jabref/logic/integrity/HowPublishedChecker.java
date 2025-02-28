package org.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class HowPublishedChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[^a-z]").asPredicate();

    private final BibDatabaseContext databaseContext;

    public HowPublishedChecker(BibDatabaseContext databaseContext) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
    }

    /**
     * Official BibTeX specification:
     *  HowPublished: How something strange has been published. The first word should be capitalized.
     * biblatex package documentation (Section 4.9.1):
     *  The biblatex package will automatically capitalize the first word when required at the beginning of a sentence.
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // BibTeX
        if (!databaseContext.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Optional.of(IntegrityIssue.SHOULD_HAVE_THE_FIRST_CHARACTER_CAPITALIZED.getText());
        }

        return Optional.empty();
    }
}
