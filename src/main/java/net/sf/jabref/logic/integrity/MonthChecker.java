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

public class MonthChecker extends FieldChecker {

    private static final Predicate<String> ONLY_AN_INTEGER = Pattern.compile("[1-9]|10|11|12")
            .asPredicate();
    private static final Predicate<String> MONTH_NORMALIZED = Pattern
            .compile("#jan#|#feb#|#mar#|#apr#|#may#|#jun#|#jul#|#aug#|#sep#|#oct#|#nov#|#dec#")
            .asPredicate();

    private final BibDatabaseContext bibDatabaseContextMonth;


    public MonthChecker(BibDatabaseContext bibDatabaseContext) {
        super(FieldName.MONTH);
        this.bibDatabaseContextMonth = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * BibLaTeX package documentation (Section 2.3.9):
     * The month field is an integer field.
     * The bibliography style converts the month to a language-dependent string as required.
     * For backwards compatibility, you may also use the following three-letter abbreviations in the month field:
     * jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec.
     * Note that these abbreviations are BibTeX strings which must be given without any braces or quotes.
     */
    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        //BibLaTeX
        if (bibDatabaseContextMonth.isBiblatexMode()
                && !(ONLY_AN_INTEGER.test(value.trim()) || MONTH_NORMALIZED.test(value.trim()))) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should be an integer or normalized"), entry, FieldName.MONTH));
        }

        //BibTeX
        if (!bibDatabaseContextMonth.isBiblatexMode() && !MONTH_NORMALIZED.test(value.trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should be normalized"), entry, FieldName.MONTH));
        }

        return Collections.emptyList();
    }
}
