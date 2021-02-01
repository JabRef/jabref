package org.jabref.gui.autocompleter;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

import com.google.common.base.Equivalence;
import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Delivers possible completions as a list of {@link Author}s.
 */
public class PersonNameSuggestionProvider extends SuggestionProvider<Author> {

    private final Collection<Field> fields;
    private final BibDatabase database;

    PersonNameSuggestionProvider(Field field, BibDatabase database) {
        this(Collections.singletonList(Objects.requireNonNull(field)), database);
    }

    public PersonNameSuggestionProvider(Collection<Field> fields, BibDatabase database) {
        super();

        this.fields = Objects.requireNonNull(fields);
        this.database = database;
    }

    public Stream<Author> getAuthors(BibEntry entry) {
        return entry.getFieldMap()
                    .entrySet()
                    .stream()
                    .filter(fieldValuePair -> fields.contains(fieldValuePair.getKey()))
                    .map(Map.Entry::getValue)
                    .map(AuthorList::parse)
                    .flatMap(authors -> authors.getAuthors().stream());
    }

    @Override
    protected Equivalence<Author> getEquivalence() {
        return Equivalence.equals().onResultOf(Author::getLastOnly);
    }

    @Override
    protected Comparator<Author> getComparator() {
        return Comparator.comparing(Author::getNameForAlphabetization);
    }

    @Override
    protected boolean isMatch(Author candidate, AutoCompletionBinding.ISuggestionRequest request) {
        return StringUtil.containsIgnoreCase(candidate.getLastFirst(false), request.getUserText());
    }

    @Override
    public Stream<Author> getSource() {
        return database.getEntries()
                       .parallelStream()
                       .flatMap(this::getAuthors);
    }
}
