package org.jabref.gui.fieldeditors.optioneditors;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.AbstractEditorViewModel;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public OptionEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
    }

    /**
     * Converts the user input to a String used in BibTeX
     */
    public abstract StringConverter<T> getStringConverter();

    /**
     * Returns all available items
     */
    public abstract Collection<T> getItems();

    /**
     * Used for filling the ComboBox for selecting a value. Needs to return something meaningful for each item in {@link #getItems()}
     */
    public abstract String convertToDisplayText(T object);
}
