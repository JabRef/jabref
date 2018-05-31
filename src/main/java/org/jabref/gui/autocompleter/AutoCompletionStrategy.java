package org.jabref.gui.autocompleter;

public interface AutoCompletionStrategy {

    public AutoCompletionInput analyze(String input);
}
