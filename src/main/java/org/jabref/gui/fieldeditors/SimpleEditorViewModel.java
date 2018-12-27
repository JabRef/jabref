package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AppendWordsStrategy;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;
import org.jabref.logic.integrity.FieldCheckers;

public class SimpleEditorViewModel extends AbstractEditorViewModel {

    public SimpleEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);
    }

    public AutoCompletionStrategy getAutoCompletionStrategy() {
        return new AppendWordsStrategy();
    }
}
