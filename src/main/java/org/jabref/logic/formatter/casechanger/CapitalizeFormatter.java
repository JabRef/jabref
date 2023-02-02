package org.jabref.logic.formatter.casechanger;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class CapitalizeFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Capitalize");
    }

    @Override
    public String getKey() {
        return "capitalize";
    }

    /**
     * Converts the first character of each word of the given string to a upper case (and all others to lower case), but does not change words starting with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(input);

        title.getWords().stream().forEach(Word::toUpperFirstIgnoreHyphen);

        return title.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Changes The First Letter Of All Words To Capital Case And The Remaining Letters To Lower Case.");
    }

    @Override
    public String getExampleInput() {
        return "I have {a} DREAM";
    }
}
