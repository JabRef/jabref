package org.jabref.logic.formatter.casechanger;

import java.util.List;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class TitleCaseFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Title case");
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
        List<String> sentences = StringUtil.getStringAsSentences(input);
        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            Title title = new Title(sentence);

            title.getWords().stream().filter(Word::isSmallerWord).forEach(Word::toLowerCase);
            title.getWords().stream().filter(Word::isLargerWord).forEach(Word::toUpperFirst);

            title.getFirstWord().ifPresent(Word::toUpperFirst);
            title.getLastWord().ifPresent(Word::toUpperFirst);

            for (int i = 0; i < (title.getWords().size() - 2); i++) {
                if (title.getWords().get(i).endsWithColon()) {
                    title.getWords().get(i + 1).toUpperFirst();
                }
            }
            result.append(title.toString() + " ");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Capitalize all words, but converts articles, prepositions, and conjunctions to lower case.");
    }

    @Override
    public String getExampleInput() {
        return "{BPMN} conformance In open source Engines";
    }
}
