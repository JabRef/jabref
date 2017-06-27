package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.util.StringConverter;

import org.jabref.logic.autocompleter.ContentAutoCompleters;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public OptionEditorViewModel(String fieldName, ContentAutoCompleters autoCompleter) {
        super(fieldName, autoCompleter);
    }

    public abstract StringConverter<T> getStringConverter();

    public abstract List<T> getItems();

    public abstract String convertToDisplayText(T object);
}
