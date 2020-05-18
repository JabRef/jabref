package org.jabref.gui.autocompleter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Enriches a suggestion provider by a given set of content selector values.
 */
public class ContentSelectorSuggestionProvider extends StringSuggestionProvider {

    private final SuggestionProvider<String> suggestionProvider;
    private final List<String> contentSelectorValues;

    public ContentSelectorSuggestionProvider(SuggestionProvider<String> suggestionProvider,
                                             List<String> contentSelectorValues) {

        this.suggestionProvider = suggestionProvider;
        this.contentSelectorValues = contentSelectorValues;
    }

    @Override
    public Stream<String> getSource() {
        return Stream.concat(contentSelectorValues.stream(), suggestionProvider.getSource());
    }
}
