package org.jabref.gui.autocompleter;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;

public class OmnisearchSuggestionProvider {

    private final TitleSuggestionProvider titleSuggestionProvider;
    private final PersonNameSuggestionProvider authorSuggestionProvider;

    public OmnisearchSuggestionProvider(Collection<Field> fields, BibDatabase database) {
        this.titleSuggestionProvider = new TitleSuggestionProvider(database);
        this.authorSuggestionProvider = new PersonNameSuggestionProvider(fields, database);
    }

    public final Collection<String> provideSuggestions(ISuggestionRequest request) {
        Stream<String> titles = titleSuggestionProvider.provideSuggestions(request).stream().map(t -> t.getTitle().get());
        Stream<String> authors = authorSuggestionProvider.provideSuggestions(request).stream().map(a -> a.getLastFirst(false));
        return Stream.concat(titles, authors).collect(Collectors.toList());
    }
}
