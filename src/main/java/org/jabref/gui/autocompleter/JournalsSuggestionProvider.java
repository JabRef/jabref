package org.jabref.gui.autocompleter;

import java.util.stream.Stream;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.Streams;

public class JournalsSuggestionProvider extends FieldValueSuggestionProvider {

    private final JournalAbbreviationRepository repository;

    JournalsSuggestionProvider(Field field, BibDatabase database, JournalAbbreviationRepository repository) {
        super(field, database);

        this.repository = repository;
    }

    @Override
    public Stream<String> getSource() {
        return Streams.concat(super.getSource(), repository.getFullNames().stream());
    }
}
