package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class UpperCaseChanger implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("UPPER");
    }

    /**
     * Converts all characters of the given string to upper case, but does not change words starting with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(input);

        title.getWords().stream().forEach(Word::toUpperCase);

        return title.toString();
    }
}
