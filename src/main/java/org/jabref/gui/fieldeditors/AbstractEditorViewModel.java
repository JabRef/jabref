package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected final String fieldName;
    protected StringProperty text = new SimpleStringProperty("");
    protected BibEntry entry;
    private final AutoCompleteSuggestionProvider<?> suggestionProvider;
    private ObjectBinding<String> fieldBinding;

    public AbstractEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider) {
        this.fieldName = fieldName;
        this.suggestionProvider = suggestionProvider;
    }

    public StringProperty textProperty() {
        return text;
    }

    public void bindToEntry(BibEntry entry) {
        this.entry = entry;

        // We need to keep a reference to the binding since it otherwise gets discarded
        fieldBinding = entry.getFieldBinding(fieldName);

        BindingsHelper.bindBidirectional(
                this.textProperty(),
                fieldBinding,
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
