package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class EditionChecker implements Checker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();
    private static final Predicate<String> ONLY_NUMERALS_OR_LITERALS = Pattern.compile("^([0-9]+|[^0-9]+)$")
            .asPredicate();

    private final BibDatabaseContext bibDatabaseContextEdition;


    public EditionChecker(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * Checks, if field contains only an integer or a literal (BibLaTeX mode)
     * Checks, if the first letter is capitalized (BibTeX mode)
     * BibLaTex:
     * The edition of a printed publication. This must be an integer, not an ordinal.
     * It is also possible to give the edition as a literal string, for example "Third, revised and expanded edition".
     * Official bibtex spec:
     * The edition of a book-for example, "Second".
     * This should be an ordinal, and should have the first letter capitalized.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.EDITION);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        //BibLaTeX
        if (!ONLY_NUMERALS_OR_LITERALS.test(value.get().trim()) && (bibDatabaseContextEdition.isBiblatexMode())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain an integer or a literal"), entry, FieldName.EDITION));
        }

        //BibTeX
        if (!FIRST_LETTER_CAPITALIZED.test(value.get().trim()) && (!bibDatabaseContextEdition.isBiblatexMode())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should have the first letter capitalized"), entry, FieldName.EDITION));
        }

        return Collections.emptyList();
    }
}
