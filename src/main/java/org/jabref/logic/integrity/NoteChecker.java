package org.jabref.logic.integrity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class NoteChecker implements ValueChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[^a-z]").asPredicate();

    private final BibDatabaseContext bibDatabaseContextEdition;

    public NoteChecker(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * biblatex package documentation (Section 4.9.1):
     * The biblatex package will automatically capitalize the first word when required at the beginning of a sentence.
     * Official BibTeX specification:
     * note: Any additional information that can help the reader. The first word should be capitalized.
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Optional.of(IntegrityIssue.SHOULD_HAVE_THE_FIRST_CHARACTER_CAPITALIZED.getText());
        }

        return Optional.empty();
    }
}
