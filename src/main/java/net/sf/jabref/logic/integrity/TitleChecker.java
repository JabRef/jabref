package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class TitleChecker implements Checker {

    private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}\\{]*\\}");
    private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();


    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.TITLE);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        /*
         * Algorithm:
         * - remove trailing whitespaces
         * - ignore first letter as this can always be written in caps
         * - remove everything that is in brackets
         * - check if at least one capital letter is in the title
         */
        String valueTrimmed = value.get().trim();
        String valueIgnoringFirstLetter = valueTrimmed.startsWith("{") ? valueTrimmed : valueTrimmed.substring(1);
        String valueOnlySpacesWithinCurlyBraces = valueIgnoringFirstLetter;
        while (true) {
            Matcher matcher = INSIDE_CURLY_BRAKETS.matcher(valueOnlySpacesWithinCurlyBraces);
            if (!matcher.find()) {
                break;
            }
            valueOnlySpacesWithinCurlyBraces = matcher.replaceAll("");
        }

        boolean hasCapitalLettersThatBibtexWillConvertToSmallerOnes = HAS_CAPITAL_LETTERS
                .test(valueOnlySpacesWithinCurlyBraces);

        if (hasCapitalLettersThatBibtexWillConvertToSmallerOnes) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("capital letters are not masked using curly brackets {}"),
                            entry, FieldName.TITLE));
        }

        return Collections.emptyList();
    }
}
