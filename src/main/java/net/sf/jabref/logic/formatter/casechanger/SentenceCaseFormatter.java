package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class SentenceCaseFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Sentence case");
    }

    @Override
    public String getKey() {
        return "sentence_case";
    }

    /**
     * Converts the first character of the first word of the given string to upper case (and the remaining characters of the first word to lower case), but does not change anything if word starts with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(new LowerCaseFormatter().format(input));

        title.getWords().stream().findFirst().ifPresent(Word::toUpperFirst);

        return title.toString();
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
