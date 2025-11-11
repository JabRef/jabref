package org.jabref.gui.autocompleter;

import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

/**
 * Stores the full content of one field.
 */
class FieldValueSuggestionProvider extends StringSuggestionProvider {

    private final Field field;
    private final BibDatabase database;

    FieldValueSuggestionProvider(Field field, BibDatabase database) {
        this.field = Objects.requireNonNull(field);
        this.database = database;
    }

    @Override
    public Stream<String> getSource() {
        return database.getEntries().parallelStream().flatMap(entry -> entry.getField(field).stream());
    }
}
