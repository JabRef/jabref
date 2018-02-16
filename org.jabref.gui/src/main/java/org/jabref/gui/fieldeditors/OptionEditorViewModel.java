package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public OptionEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);
    }

    public abstract StringConverter<T> getStringConverter();

    public abstract List<T> getItems();

    public abstract String convertToDisplayText(T object);
}
