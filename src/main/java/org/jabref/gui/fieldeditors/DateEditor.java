package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class DateEditor extends HBox implements FieldEditorFX {

    @FXML private DateEditorViewModel viewModel;
    @FXML private TemporalAccessorPicker datePicker;

    @Inject private UndoManager undoManager;
    @Inject private GuiPreferences preferences;
    @Inject private KeyBindingRepository keyBindingRepository;

    public DateEditor(Field field,
                      DateTimeFormatter dateFormatter,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers,
                      UndoAction undoAction,
                      RedoAction redoAction) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new DateEditorViewModel(field, suggestionProvider, dateFormatter, fieldCheckers, undoManager);
        datePicker.setStringConverter(viewModel.getDateToStringConverter());
        establishBinding(datePicker.getEditor(), viewModel.textProperty(), keyBindingRepository, undoAction, redoAction);
        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), datePicker.getEditor());
    }

    public DateEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
