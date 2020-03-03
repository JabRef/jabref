package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class TitleChecker implements ValueChecker {

    private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}\\{]*\\}");
    private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();

    private final BibDatabaseContext databaseContext;

    public TitleChecker(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    /**
     * Algorithm:
     * - remove everything that is in brackets
     * - split the title into subTitles based on the delimiters
     * - for each subTitle:
     * -    remove trailing whitespaces
     * -    ignore first letter as this can always be written in caps
     * -    check if at least one capital letter is in the title
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (databaseContext.isBiblatexMode()) {
            return Optional.empty();
        }

        String valueOnlySpacesWithinCurlyBraces = value;
        while (true) {
            Matcher matcher = INSIDE_CURLY_BRAKETS.matcher(valueOnlySpacesWithinCurlyBraces);
            if (!matcher.find()) {
                break;
            }
            valueOnlySpacesWithinCurlyBraces = matcher.replaceAll("");
        }

        // Delimiters are . ! ? ; :
        String[] delimitedStr = valueOnlySpacesWithinCurlyBraces.split("(\\.|\\!|\\?|\\;|\\:)");
        for (String subStr : delimitedStr) {
            subStr = subStr.trim();
            if (subStr.length() > 0) {
                subStr = subStr.substring(1);
            }
            boolean hasCapitalLettersThatBibtexWillConvertToSmallerOnes = HAS_CAPITAL_LETTERS.test(subStr);
            if (hasCapitalLettersThatBibtexWillConvertToSmallerOnes) {
                return Optional.of(Localization.lang("capital letters are not masked using curly brackets {}"));
            }
        }
        return Optional.empty();
    }
}
