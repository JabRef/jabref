package org.jabref.gui.autocompleter;

import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

/**
 * Stores all words in the given field.
 */
public class WordSuggestionProvider extends StringSuggestionProvider {

    private final Field field;
    private final BibDatabase database;

    public WordSuggestionProvider(Field field, BibDatabase database) {
        this.field = Objects.requireNonNull(field);
        this.database = database;
    }

    @Override
    public Stream<String> getSource() {
        return database.getEntries()
                       .parallelStream()
                       .flatMap(entry -> entry.getFieldAsWords(field).stream());
    }
}
