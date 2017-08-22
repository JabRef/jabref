package org.jabref.gui.autocompleter;

import java.util.Objects;
import java.util.StringTokenizer;

import org.jabref.model.entry.BibEntry;

/**
 * Stores all words in the given field which are separated by SEPARATING_CHARS.
 */
public class WordSuggestionProvider extends StringSuggestionProvider implements AutoCompleteSuggestionProvider<String> {

    private static final String SEPARATING_CHARS = ";,\n ";

    private final String fieldName;

    public WordSuggestionProvider(String fieldName) {
        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public void indexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(fieldName).ifPresent(fieldValue -> {
            StringTokenizer tok = new StringTokenizer(fieldValue, SEPARATING_CHARS);
            while (tok.hasMoreTokens()) {
                addPossibleSuggestions(tok.nextToken());
            }
        });
    }
}
