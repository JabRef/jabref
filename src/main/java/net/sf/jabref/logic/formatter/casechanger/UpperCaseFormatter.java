package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class UpperCaseFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Upper case");
    }

    @Override
    public String getKey() {
        return "upper_case";
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

    @Override
    public String getDescription() {
        return Localization.lang(
                "Changes all letters to upper case.");
    }

    @Override
    public String getExampleInput() {
        return "Kde {Amarok}";
    }

}
