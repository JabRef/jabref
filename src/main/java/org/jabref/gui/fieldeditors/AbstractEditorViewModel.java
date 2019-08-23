package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.JabRefGUI;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.integrity.ValueChecker;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.controlsfx.control.textfield.AutoCompletionBinding;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected final Field field;
    protected StringProperty text = new SimpleStringProperty("");
    protected BibEntry entry;
    private final AutoCompleteSuggestionProvider<?> suggestionProvider;
    private final CompositeValidator fieldValidator;
    private ObjectBinding<String> fieldBinding;

    public AbstractEditorViewModel(Field field, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.field = field;
        this.suggestionProvider = suggestionProvider;

        this.fieldValidator = new CompositeValidator();
        for (ValueChecker checker : fieldCheckers.getForField(field)) {
            FunctionBasedValidator<String> validator = new FunctionBasedValidator<>(text, value ->
                    checker.checkValue(value).map(ValidationMessage::warning).orElse(null));
            fieldValidator.addValidators(validator);
        }
    }

    public Validator getFieldValidator() {
        return fieldValidator;
    }

    public StringProperty textProperty() {
        return text;
    }

    public void bindToEntry(BibEntry entry) {
        this.entry = entry;

        // We need to keep a reference to the binding since it otherwise gets discarded
        fieldBinding = entry.getFieldBinding(field);

        BindingsHelper.bindBidirectional(
                this.textProperty(),
                fieldBinding,
                newValue -> {
                    if (newValue != null) {
                        String oldValue = entry.getField(field).orElse(null);
                        entry.setField(field, newValue);
                        UndoManager undoManager = JabRefGUI.getMainFrame().getUndoManager();
                        undoManager.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
                    }
                });
    }

    public Collection<?> complete(AutoCompletionBinding.ISuggestionRequest request) {
        return suggestionProvider.call(request);
    }
}
