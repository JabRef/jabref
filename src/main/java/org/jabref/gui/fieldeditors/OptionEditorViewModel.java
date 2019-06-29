package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public OptionEditorViewModel(Field field, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
    }

    public abstract StringConverter<T> getStringConverter();

    public abstract List<T> getItems();

    public abstract String convertToDisplayText(T object);
}
