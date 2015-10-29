package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.Formatter;

public class UpperEachFirstCaseChanger implements Formatter {

    @Override
    public String getName() {
        return "Upper Each First";
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
