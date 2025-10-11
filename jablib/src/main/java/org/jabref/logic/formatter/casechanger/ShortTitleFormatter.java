package org.jabref.logic.formatter.casechanger;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

public class ShortTitleFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Short title");
    }

    @Override
    public String getKey() {
        return "short_title";
    }

    @Override
    public String format(@NonNull String input) {
        Title title = new Title(input);

        return title.getWords().stream()
                    .filter(Predicate.not(Word::isSmallerWord))
                    .map(Word::toString)
                    .limit(3)
                    .collect(Collectors.joining(" "));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Returns first 3 words of the title ignoring any function words.");
    }

    @Override
    public String getExampleInput() {
        return "This is a short title";
    }
}
