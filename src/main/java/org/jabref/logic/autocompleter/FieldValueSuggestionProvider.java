package org.jabref.logic.autocompleter;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

/**
 * Stores the full content of one field.
 */
class FieldValueSuggestionProvider extends StringSuggestionProvider implements AutoCompleteSuggestionProvider<String> {

    private final String fieldName;

    FieldValueSuggestionProvider(String fieldName) {
        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public void indexBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(fieldName).ifPresent(fieldValue -> addPossibleSuggestions(fieldValue.trim()));
    }
}
