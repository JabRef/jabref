package org.jabref.gui.autocompleter;

public class AppendPersonNamesStrategy extends AppendWordsStrategy {

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
    public String getDelimiter() {
        if (this.separationBySpace) {
            return " ";
        } else {
            return " and ";
        }
    }
}
