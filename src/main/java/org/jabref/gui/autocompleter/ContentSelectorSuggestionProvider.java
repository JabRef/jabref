package org.jabref.gui.autocompleter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.util.Callback;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Enriches a suggestion provider by a given set of content selector values.
 */
public class ContentSelectorSuggestionProvider implements Callback<AutoCompletionBinding.ISuggestionRequest, Collection<String>> {

    private final AutoCompleteSuggestionProvider<String> suggestionProvider;
    private final List<String> contentSelectorValues;

    public ContentSelectorSuggestionProvider(AutoCompleteSuggestionProvider<String> suggestionProvider,
                                             List<String> contentSelectorValues) {

        this.suggestionProvider = suggestionProvider;
        this.contentSelectorValues = contentSelectorValues;
    }

    @Override
    public Collection<String> call(AutoCompletionBinding.ISuggestionRequest request) {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(suggestionProvider.call(request));
        suggestions.addAll(contentSelectorValues);
        return suggestions;
    }
}
