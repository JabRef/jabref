package org.jabref.logic.formatter.casechanger;

import java.util.Optional;
import java.util.function.Predicate;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

public class VeryShortTitleFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Very short title");
    }

    @Override
    public String getKey() {
        return "very_short_title";
    }

    @Override
    public String format(@NonNull String input) {
        Title title = new Title(input);

        Optional<Word> resultTitle = title.getWords().stream()
                                          .filter(Predicate.not(Word::isSmallerWord))
                                          .findFirst();

        return resultTitle.map(Word::toString).orElse("");
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Returns first word of the title ignoring any function words.");
    }

    @Override
    public String getExampleInput() {
        return "A very short title";
    }
}
