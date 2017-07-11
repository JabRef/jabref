package org.jabref.gui.autocompleter;

import java.util.Locale;

public class AppendPersonNamesStrategy implements AutoCompletionStrategy {

    /**
     * true if the input should be split at a single white space instead of the usual delimiter " and " for names.
     * Useful if the input consists of a list of last names.
     */
    private final boolean separationBySpace;

    public AppendPersonNamesStrategy() {
        this(false);
    }

    public AppendPersonNamesStrategy(boolean separationBySpace) {
        this.separationBySpace = separationBySpace;
    }

    @Override
    public AutoCompletionInput analyze(String input) {
        if (this.separationBySpace) {
            return determinePrefixAndReturnRemainder(input, " ");
        } else {
            return determinePrefixAndReturnRemainder(input, " and ");
        }
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
