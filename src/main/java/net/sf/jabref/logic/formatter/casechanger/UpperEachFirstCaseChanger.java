package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class UpperEachFirstCaseChanger implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Upper Each First");
    }

    @Override
    public String getKey() {
        return "UpperEachFirstCaseChanger";
    }

    /**
     * Converts the first character of each word of the given string to a upper case (and all others to lower case), but does not change words starting with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(input);

        title.getWords().stream().forEach(Word::toUpperFirst);

        return title.toString();
    }
}
