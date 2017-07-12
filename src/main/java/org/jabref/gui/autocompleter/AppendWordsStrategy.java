package org.jabref.gui.autocompleter;

import java.util.Locale;

public class AppendWordsStrategy implements AutoCompletionStrategy {

    protected String getDelimiter() {
        return " ";
    }

    @Override
    public AutoCompletionInput analyze(String input) {
        return determinePrefixAndReturnRemainder(input, getDelimiter());
    }

    private AutoCompletionInput determinePrefixAndReturnRemainder(String input, String delimiter) {
        int index = input.toLowerCase(Locale.ROOT).lastIndexOf(delimiter);
        if (index >= 0) {
            String prefix = input.substring(0, index + delimiter.length());
            String rest = input.substring(index + delimiter.length());
            return new AutoCompletionInput(prefix, rest);
        } else {
            return new AutoCompletionInput("", input);
        }
    }
}
