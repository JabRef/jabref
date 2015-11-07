package net.sf.jabref.logic.formatter.casechanger;

import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;

public class UpperFirstCaseChanger implements Formatter {

    @Override
    public String getName() {
        return "Upper first";
    }

    /**
     * Converts the first character of the first word of the given string to a upper case (and the remaining characters of the first word to lower case), but does not change anything if word starts with "{"
     */
    @Override
    public String format(String input) {
        Title title = new Title(CaseChangers.LOWER.format(input));

        title.getWords().stream().findFirst().ifPresent(Word::toUpperFirst);

        return title.toString();
    }
}
