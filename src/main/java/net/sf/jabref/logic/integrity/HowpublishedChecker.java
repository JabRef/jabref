package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class HowpublishedChecker extends FieldChecker {

    private static final Predicate<String> FIRST_LETTER_CAPITALIZED = Pattern.compile("^[A-Z]").asPredicate();

    private final BibDatabaseContext bibDatabaseContextEdition;


    public HowpublishedChecker(BibDatabaseContext bibDatabaseContext) {
        super(FieldName.HOWPUBLISHED);
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * BibLaTeX package documentation (Section 4.9.1):
     * The BibLaTeX package will automatically capitalize the first word when required at the beginning of a sentence.
     * Official BibTeX specification:
     * howpublished: How something strange has been published. The first word should be capitalized.
     */
    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        //BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode() && !FIRST_LETTER_CAPITALIZED.test(value.trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should have the first letter capitalized"), entry, FieldName.HOWPUBLISHED));
        }

        return Collections.emptyList();
    }
}
