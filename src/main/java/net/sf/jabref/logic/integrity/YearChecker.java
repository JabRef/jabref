package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class YearChecker extends FieldChecker {

    private static final Predicate<String> CONTAINS_FOUR_DIGIT = Pattern.compile("([^0-9]|^)[0-9]{4}([^0-9]|$)")
            .asPredicate();
    private static final Predicate<String> ENDS_WITH_FOUR_DIGIT = Pattern.compile("[0-9]{4}$").asPredicate();
    private static final String PUNCTUATION_MARKS = "[(){},.;!?<>%&$]";

    public YearChecker() {
        super(FieldName.YEAR);
    }

    /**
     * Checks, if the number String contains a four digit year and ends with it.
     * Official bibtex spec:
     * Generally it should consist of four numerals, such as 1984, although the standard styles
     * can handle any year whose last four nonpunctuation characters are numerals, such as ‘(about 1984)’.
     * Source: http://ftp.fernuni-hagen.de/ftp-dir/pub/mirrors/www.ctan.org/biblio/bibtex/base/btxdoc.pdf
     */
    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (!CONTAINS_FOUR_DIGIT.test(value.trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a four digit number"), entry, FieldName.YEAR));
        }

        if (!ENDS_WITH_FOUR_DIGIT.test(value.replaceAll(PUNCTUATION_MARKS, ""))) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("last four nonpunctuation characters should be numerals"),
                            entry, FieldName.YEAR));
        }

        return Collections.emptyList();
    }
}
