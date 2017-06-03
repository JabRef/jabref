package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.util.StringConverter;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public abstract StringConverter<T> getStringConverter();

    public abstract List<T> getItems();

    public abstract String convertToDisplayText(T object);
}
