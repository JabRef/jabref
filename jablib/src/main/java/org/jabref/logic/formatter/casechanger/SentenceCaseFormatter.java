package org.jabref.logic.formatter.casechanger;

import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class SentenceCaseFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Sentence case");
    }

    @Override
    public String getKey() {
        return "sentence_case";
    }

    /**
     * Converts the first character of the first word of the given string to upper case (and the remaining characters of the first word to lower case) and changes other words to lower case, but does not change anything if word starts with "{"
     */
    @Override
    public String format(String input) {
        return StringUtil.getStringAsSentences(input)
                .stream()
                .map(new LowerCaseFormatter()::format)
                .map(Title::new)
                .map(title -> {
                    title.getFirstWord().ifPresent(Word::toUpperFirst);
                    return title;
                })
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Capitalize the first word, changes other words to lower case.");
    }

    @Override
    public String getExampleInput() {
        return "i have {Aa} DREAM";
    }
}
