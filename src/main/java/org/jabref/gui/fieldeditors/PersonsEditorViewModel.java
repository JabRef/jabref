package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.Author;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class PersonsEditorViewModel extends AbstractEditorViewModel {

    private final AutoCompletePreferences preferences;

    public PersonsEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, AutoCompletePreferences preferences, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);
        this.preferences = preferences;
    }

    public StringConverter<Author> getAutoCompletionConverter() {
        return new PersonNameStringConverter(preferences);
    }

    @SuppressWarnings("unchecked")
    public Collection<Author> complete(AutoCompletionBinding.ISuggestionRequest request) {
        return (Collection<Author>) super.complete(request);
    }

    public AutoCompletionStrategy getAutoCompletionStrategy() {
        return new AppendPersonNamesStrategy();
    }
}
