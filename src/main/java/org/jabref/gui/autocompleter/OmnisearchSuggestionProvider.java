package org.jabref.gui.autocompleter;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Suggestion;
import org.jabref.model.entry.field.Field;

import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;

public class OmnisearchSuggestionProvider {

    private final TitleSuggestionProvider titleSuggestionProvider;
    private final PersonNameSuggestionProvider authorSuggestionProvider;

    public OmnisearchSuggestionProvider(Collection<Field> fields, BibDatabase database) {
        this.titleSuggestionProvider = new TitleSuggestionProvider(database);
        this.authorSuggestionProvider = new PersonNameSuggestionProvider(fields, database);
    }

    public final Collection<Suggestion> provideSuggestions(ISuggestionRequest request) {
        Stream<Suggestion> authors = Stream.concat(Stream.of(new Suggestion("Author/Editor",String.class)),authorSuggestionProvider.provideSuggestions(request).stream().map(a -> new Suggestion(a.getLastFirst(false), Author.class)));
        Stream<Suggestion> titles = Stream.concat(Stream.of(new Suggestion("Title",String.class)),titleSuggestionProvider.provideSuggestions(request).stream().map(t -> new Suggestion(t.getTitle().get(), BibEntry.class)));
        return Stream.concat(titles, authors).collect(Collectors.toList());
    }
}
