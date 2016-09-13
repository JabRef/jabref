package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class YearChecker implements Checker {

    private static final Predicate<String> CONTAINS_FOUR_DIGIT = Pattern.compile("([^0-9]|^)[0-9]{4}([^0-9]|$)")
            .asPredicate();
    private static final Predicate<String> ENDS_WITH_FOUR_DIGIT = Pattern.compile("[0-9]{4}$").asPredicate();
    private static final String PUNCTUATION_MARKS = "[(){},.;!?<>%&$]";


    /**
     * Checks, if the number String contains a four digit year and ends with it.
     * Official bibtex spec:
     * Generally it should consist of four numerals, such as 1984, although the standard styles
     * can handle any year whose last four nonpunctuation characters are numerals, such as ‘(about 1984)’.
     * Source: http://ftp.fernuni-hagen.de/ftp-dir/pub/mirrors/www.ctan.org/biblio/bibtex/base/btxdoc.pdf
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.YEAR);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if (!CONTAINS_FOUR_DIGIT.test(value.get().trim())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a four digit number"), entry, FieldName.YEAR));
        }

        if (!ENDS_WITH_FOUR_DIGIT.test(value.get().replaceAll(PUNCTUATION_MARKS, ""))) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("last four nonpunctuation characters should be numerals"),
                            entry, FieldName.YEAR));
        }

        return Collections.emptyList();
    }
}
