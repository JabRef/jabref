package org.jabref.logic.formatter.casechanger;

import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class TitleCaseFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Title Case");
    }

    @Override
    public String getKey() {
        return "title_case";
    }

    /**
     * Converts all words to upper case, but converts articles, prepositions, and conjunctions to lower case
     * Capitalizes first and last word
     * Does not change words starting with "{"
     */
    @Override
    public String format(String input) {
        return StringUtil.getStringAsSentences(input)
                .stream()
                .map(sentence -> {
                    Title title = new Title(sentence);

                    title.getWords().stream().filter(Word::isSmallerWord).forEach(Word::toLowerCase);
                    title.getWords().stream().filter(Word::isLargerWord).forEach(Word::toUpperFirstTitle);

                    title.getFirstWord().ifPresent(Word::toUpperFirstTitle);
                    title.getLastWord().ifPresent(Word::toUpperFirstTitle);

                    for (int i = 0; i < (title.getWords().size() - 2); i++) {
                        if (title.getWords().get(i).endsWithColon()) {
                            title.getWords().get(i + 1).toUpperFirstTitle();
                        }
                    }

                    return title.toString();
                })
                .collect(Collectors.joining(" "));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Capitalize all Words, but Converts Articles, Prepositions, and Conjunctions to Lower Case.");
    }

    @Override
    public String getExampleInput() {
        return "{BPMN} conformance In open source Engines";
    }
}
