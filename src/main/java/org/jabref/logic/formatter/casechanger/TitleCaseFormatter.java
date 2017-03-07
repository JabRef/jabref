package org.jabref.logic.formatter.casechanger;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

public class TitleCaseFormatter implements Formatter {

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
        Title title = new Title(input);

        title.getWords().stream().filter(Word::isSmallerWord).forEach(Word::toLowerCase);
        title.getWords().stream().filter(Word::isLargerWord).forEach(Word::toUpperFirst);

        title.getFirstWord().ifPresent(Word::toUpperFirst);
        title.getLastWord().ifPresent(Word::toUpperFirst);

        for (int i = 0; i < (title.getWords().size() - 2); i++) {
            if (title.getWords().get(i).endsWithColon()) {
                title.getWords().get(i + 1).toUpperFirst();
            }
        }

        return title.toString();
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
