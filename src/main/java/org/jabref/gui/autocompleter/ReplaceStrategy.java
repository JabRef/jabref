package org.jabref.gui.autocompleter;

public class ReplaceStrategy implements AutoCompletionStrategy {

    @Override
    public AutoCompletionInput analyze(String input) {
        return new AutoCompletionInput("", input);
    }
}
