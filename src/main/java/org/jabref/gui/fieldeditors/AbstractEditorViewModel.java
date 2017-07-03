package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected final String fieldName;
    protected StringProperty text = new SimpleStringProperty("");
    protected BibEntry entry;
    private final AutoCompleteSuggestionProvider<?> suggestionProvider;

    public AbstractEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider) {
        this.fieldName = fieldName;
        this.suggestionProvider = suggestionProvider;
    }

    public StringProperty textProperty() {
        return text;
    }

    public void bindToEntry(BibEntry entry) {
        this.entry = entry;
        BindingsHelper.bindBidirectional(
                this.textProperty(),
                entry.getFieldBinding(fieldName),
                newValue -> {
                    if (newValue != null) {
                        entry.setField(fieldName, newValue);
                    }
                });
    }

    public Collection<?> complete(AutoCompletionBinding.ISuggestionRequest request) {
        return suggestionProvider.call(request);
    }
}
