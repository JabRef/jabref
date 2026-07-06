package org.jabref.gui.fieldeditors;

import java.util.Collection;
import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.tobiasdiez.easybind.EasyObservableValue;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.jfxcore.validation.Constraint;
import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected final Field field;
    protected final ConstrainedStringProperty<ValidationMessage> text;
    protected BibEntry entry;
    protected final UndoManager undoManager;

    private final SuggestionProvider<?> suggestionProvider;
    private EasyObservableValue<String> fieldBinding;

    @SuppressWarnings("unchecked")
    public AbstractEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        this.field = field;
        this.suggestionProvider = suggestionProvider;
        this.undoManager = undoManager;

        List<Constraint<? super String, ValidationMessage>> constraints = fieldCheckers.getForField(field).stream()
                .<Constraint<? super String, ValidationMessage>>map(checker -> ValidationConstraints.function(value ->
                        checker.checkValue(value).map(ValidationMessage::warning)))
                .toList();
        this.text = new SimpleConstrainedStringProperty<>("", constraints.toArray(new Constraint[0]));
    }

    public ConstrainedStringProperty<ValidationMessage> textProperty() {
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
