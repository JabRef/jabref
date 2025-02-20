package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.gui.integrity.IntegrityIssue;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

public class TitleChecker implements ValueChecker {

    private static final Pattern INSIDE_CURLY_BRAKETS = Pattern.compile("\\{[^}\\{]*\\}");
    private static final Pattern DELIMITERS = Pattern.compile("\\.|\\!|\\?|\\;|\\:|\\[");
    private static final Predicate<String> HAS_CAPITAL_LETTERS = Pattern.compile("[\\p{Lu}\\p{Lt}]").asPredicate();

    private final BibDatabaseContext databaseContext;

    public TitleChecker(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    /**
     * Algorithm:
     * - remove everything that is in curly brackets
     * - split the title into subtitles based on the delimiters
     * (defined in the local variable DELIMITERS, currently . ! ? ; : [)
     * - for each sub title:
     * -    remove trailing whitespaces
     * -    ignore first letter as this can always be written in caps
     * -    check if at least one capital letter is in the subtitle
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (databaseContext.isBiblatexMode()) {
            return Optional.empty();
        }

        String valueOnlySpacesWithinCurlyBraces = INSIDE_CURLY_BRAKETS.matcher(value).replaceAll("");

        String[] splitTitle = DELIMITERS.split(valueOnlySpacesWithinCurlyBraces);
        for (String subTitle : splitTitle) {
            subTitle = subTitle.trim();
            if (!subTitle.isEmpty()) {
                subTitle = subTitle.substring(1);
                if (HAS_CAPITAL_LETTERS.test(subTitle)) {
                    return Optional.of(IntegrityIssue.CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS.getText());
                }
            }
        }

        return Optional.empty();
    }
}
