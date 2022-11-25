package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompletionStrategy;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.field.Field;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class PersonsEditorViewModel extends AbstractEditorViewModel {

    private final AutoCompletePreferences preferences;

    public PersonsEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, AutoCompletePreferences preferences, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
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
