package net.sf.jabref.gui.autocompleter;

import java.util.List;
import java.util.Objects;

public class AutoCompleteState {
    private final List<String> suggestions;
    private String currentWord;
    private int currentCompletion;
    private int realCaretPosition;

    public AutoCompleteState(String currentWord, List<String> suggestions, int caretPosition) {
        this.currentWord = Objects.requireNonNull(currentWord);
        this.suggestions = Objects.requireNonNull(suggestions);
        realCaretPosition = caretPosition;
        currentCompletion = 0;
    }

    public String nextSuggestion() {
        return cycleSuggestion(1);
    }

    public String previousSuggestion() {
        return cycleSuggestion(-1);
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public boolean continuesSuggestion(char newChar) {
        return !getSuggestedWord().startsWith(currentWord + newChar);
    }

    public void updateState(char newChar) {
        if (continuesSuggestion(newChar)) {
            currentWord = currentWord + newChar;
            realCaretPosition++;
        } else {
            throw new IllegalArgumentException("Make sure continuesSuggestion(newChar) is true before using this method!");
        }
    }

    public int getRealCaretPosition() {
        return realCaretPosition;
    }

    public String getSuggestedWord() {
        return suggestions.get(currentCompletion);
    }

    private String cycleSuggestion(int increment) {
        currentCompletion += increment;

        if (currentCompletion >= suggestions.size()) {
            currentCompletion = 0;
        } else if (currentCompletion < 0) {
            currentCompletion = suggestions.size() - 1;
        }

        return getSuggestedWord();
    }
}
