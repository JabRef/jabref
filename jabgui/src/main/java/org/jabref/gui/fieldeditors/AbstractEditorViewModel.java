package org.jabref.gui.fieldeditors;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.integrity.ValueChecker;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.tobiasdiez.easybind.EasyObservableValue;
import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.controlsfx.control.textfield.AutoCompletionBinding;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected final Field field;
    protected StringProperty text = new SimpleStringProperty("");
    protected BibEntry entry;
    protected final UndoManager undoManager;

    private final SuggestionProvider<?> suggestionProvider;
    private final CompositeValidator fieldValidator;
    private EasyObservableValue<String> fieldBinding;

    public AbstractEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        this.field = field;
        this.suggestionProvider = suggestionProvider;
        this.undoManager = undoManager;

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
        fieldBinding = entry.getFieldBinding(field).asOrdinary();

        BindingsHelper.bindBidirectional(
                this.textProperty(),
                fieldBinding,
                newValue -> {
                    if (newValue != null) {
                        // A file may be loaded using CRLF. ControlsFX uses hardcoded \n for multiline fields.
                        // Thus, we need to normalize the line endings.
                        // Note: Normalizing for the .bib file is done during writing of the .bib file (see org.jabref.logic.exporter.BibWriter.BibWriter).
                        String oldValue = entry.getField(field).map(value -> value.replace("\r\n", "\n")).orElse(null);
                        if (!newValue.equals(oldValue)) {
                            entry.setField(field, newValue);
                            undoManager.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
                        }
                    }
                });
    }

    public Collection<?> complete(AutoCompletionBinding.ISuggestionRequest request) {
        return suggestionProvider.provideSuggestions(request);
    }
}
