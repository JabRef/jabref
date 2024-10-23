package org.jabref.logic.formatter.casechanger;

import java.util.Optional;
import java.util.function.Predicate;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Converts all characters of the given string to upper case, but does not change words starting with "{"
 */
public class VeryShortTitleFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("VERY SHORT TITLE");
    }

    @Override
    public String getKey() {
        return "very_short_title";
    }

    @Override
    public String format(String input) {
        Title title = new Title(input);

        Optional<Word> resultTitle = title.getWords().stream()
                                          .filter(Predicate.not(
                                                  Word::isSmallerWord))
                                          .findFirst();

        return resultTitle.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "RETURNS FIRST WORD OF THE TITLE IGNORING ANY FUNCTION WORDS.");
    }

    @Override
    public String getExampleInput() {
        return "";
    }
}
