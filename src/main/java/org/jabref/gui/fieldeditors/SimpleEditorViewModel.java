package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AppendWordsStrategy;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;

public class SimpleEditorViewModel extends AbstractEditorViewModel {

    public SimpleEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider) {
        super(fieldName, suggestionProvider);
    }

    public AutoCompletionStrategy getAutoCompletionStrategy() {
        return new AppendWordsStrategy();
    }
}
