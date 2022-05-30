package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.model.entry.Suggestion;

public class SuggestionStringConverter extends StringConverter<Suggestion> {

    public SuggestionStringConverter() { }

    @Override
    public String toString(Suggestion suggestion) {
        return suggestion.getValue();
    }

    @Override
    public Suggestion fromString(String string) {
        return new Suggestion(string, String.class);
    }
}
