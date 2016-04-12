package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class LowerCaseFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Lower case");
    }

    @Override
    public String getKey() {
        return "lower_case";
    }

    /**
     * Converts all characters of the string to lower case, but does not change words starting with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(input);

        title.getWords().stream().forEach(Word::toLowerCase);

        return title.toString();
    }

    @Override
    public int hashCode() {
        return defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return defaultEquals(obj);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Changes all letters to lower case.");
    }

    @Override
    public String getExampleInput() {
        return "KDE {Amarok}";
    }

}
