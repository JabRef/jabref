package org.jabref.gui.autocompleter;

import java.util.Objects;
import java.util.StringTokenizer;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Stores all words in the given field which are separated by SEPARATING_CHARS.
 */
public class WordSuggestionProvider extends StringSuggestionProvider implements AutoCompleteSuggestionProvider<String> {

    private static final String SEPARATING_CHARS = ";,\n ";

    private final Field field;

    public WordSuggestionProvider(Field field) {
        this.field = Objects.requireNonNull(field);
    }

    @Override
    public void indexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(field).ifPresent(fieldValue -> {
            StringTokenizer tok = new StringTokenizer(fieldValue, SEPARATING_CHARS);
            while (tok.hasMoreTokens()) {
                addPossibleSuggestions(tok.nextToken());
            }
        });
    }
}
