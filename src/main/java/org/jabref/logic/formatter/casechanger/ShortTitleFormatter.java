package org.jabref.logic.formatter.casechanger;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Converts all characters of the given string to upper case, but does not change words starting with "{"
 */
public class ShortTitleFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("SHORT TITLE");
    }

    @Override
    public String getKey() {
        return "short_title";
    }

    @Override
    public String format(String input) {
        Title title = new Title(input);

        return title.getWords().stream()
                    .filter(Predicate.not(
                                        Word::isSmallerWord))
                    .map(Word::toString)
                    .limit(3)
                    .collect(Collectors.joining(" "));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "RETURNS FIRST 3 WORDS OF THE TITLE IGNORING ANY FUNCTION WORDS.");
    }

    @Override
    public String getExampleInput() {
        return "";
    }
}
