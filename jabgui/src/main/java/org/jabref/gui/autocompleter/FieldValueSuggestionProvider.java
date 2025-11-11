package org.jabref.gui.autocompleter;

import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NonNull;

/**
 * Stores the full content of one field.
 */
class FieldValueSuggestionProvider extends StringSuggestionProvider {

    private final Field field;
    private final BibDatabase database;

    FieldValueSuggestionProvider(@NonNull Field field, @NonNull BibDatabase database) {
        this.field = field;
        this.database = database;
    }

    @Override
    public Stream<String> getSource() {
        return database.getEntries().parallelStream().flatMap(entry -> entry.getField(field).stream());
    }
}
