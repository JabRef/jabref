package org.jabref.gui.autocompleter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Delivers possible completions as a list of {@link Author}s.
 */
public class PersonNameSuggestionProvider extends SuggestionProvider<Author> implements AutoCompleteSuggestionProvider<Author> {

    private final List<String> fieldNames;
    private final Comparator<Author> authorComparator = Comparator.comparing(Author::getNameForAlphabetization);

    PersonNameSuggestionProvider(String fieldName) {
        this(Collections.singletonList(Objects.requireNonNull(fieldName)));
    }

    public PersonNameSuggestionProvider(List<String> fieldNames) {
        super();

        this.fieldNames = Objects.requireNonNull(fieldNames);

    }

    @Override
    public void indexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        for (String fieldName : fieldNames) {
            entry.getField(fieldName).ifPresent(fieldValue ->  {
                AuthorList authorList = AuthorList.parse(fieldValue);
                for (Author author : authorList.getAuthors()) {
                    addPossibleSuggestions(author);
                }
            });
        }
    }

    @Override
    protected Comparator<Author> getComparator() {
        return authorComparator;
    }

    @Override
    protected boolean isMatch(Author suggestion, AutoCompletionBinding.ISuggestionRequest request) {
        String userTextLower = request.getUserText().toLowerCase();
        String suggestionStr = suggestion.getLastFirst(false).toLowerCase();
        return suggestionStr.contains(userTextLower);
    }
}
