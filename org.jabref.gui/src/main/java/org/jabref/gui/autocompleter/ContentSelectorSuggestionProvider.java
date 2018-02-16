package org.jabref.gui.autocompleter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Enriches a suggestion provider by a given set of content selector values.
 */
public class ContentSelectorSuggestionProvider implements AutoCompleteSuggestionProvider<String> {

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
        if (suggestionProvider != null) {
            suggestions.addAll(suggestionProvider.call(request));
        }
        suggestions.addAll(contentSelectorValues);
        return suggestions;
    }

    @Override
    public void indexEntry(BibEntry entry) {
        suggestionProvider.indexEntry(entry);
    }
}
