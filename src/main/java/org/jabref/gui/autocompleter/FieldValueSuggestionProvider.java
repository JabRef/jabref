package org.jabref.gui.autocompleter;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Stores the full content of one field.
 */
class FieldValueSuggestionProvider extends StringSuggestionProvider implements AutoCompleteSuggestionProvider<String> {

    private final Field field;

    FieldValueSuggestionProvider(Field field) {
        this.field = Objects.requireNonNull(field);
    }

    @Override
    public void indexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(field).ifPresent(fieldValue -> addPossibleSuggestions(fieldValue.trim()));
    }
}
