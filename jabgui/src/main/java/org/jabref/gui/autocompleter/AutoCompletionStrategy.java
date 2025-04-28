package org.jabref.gui.autocompleter;

public interface AutoCompletionStrategy {
    AutoCompletionInput analyze(String input);
}
