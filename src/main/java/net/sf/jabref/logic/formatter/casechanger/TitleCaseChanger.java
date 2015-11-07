package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class TitleCaseChanger implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Title");
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
}
