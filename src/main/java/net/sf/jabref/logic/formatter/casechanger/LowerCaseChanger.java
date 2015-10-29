package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;

public class LowerCaseChanger implements Formatter {

    @Override
    public String getName() {
        return "lower";
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
}
