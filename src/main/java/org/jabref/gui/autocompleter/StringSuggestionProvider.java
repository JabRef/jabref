package org.jabref.gui.autocompleter;

import java.util.Comparator;

import org.controlsfx.control.textfield.AutoCompletionBinding;

class StringSuggestionProvider extends SuggestionProvider<String> {

    private final Comparator<String> stringComparator = Comparator.naturalOrder();

    public StringSuggestionProvider() {

    }

    @Override
    protected Comparator<String> getComparator() {
        return stringComparator;
    }

    @Override
    protected boolean isMatch(String suggestion, AutoCompletionBinding.ISuggestionRequest request) {
        String userTextLower = request.getUserText().toLowerCase();
        String suggestionStr = suggestion.toLowerCase();
        return suggestionStr.contains(userTextLower);
    }
}
