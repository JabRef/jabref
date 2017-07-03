package org.jabref.gui.fieldeditors;

import org.jabref.logic.autocompleter.AutoCompleteSuggestionProvider;

public class SimpleEditorViewModel extends AbstractEditorViewModel {

    public SimpleEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider) {
        super(fieldName, suggestionProvider);
    }
}
