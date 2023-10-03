package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class DateEditor extends HBox implements FieldEditorFX {

    @FXML private DateEditorViewModel viewModel;
    @FXML private TemporalAccessorPicker datePicker;

    @Inject private UndoManager undoManager;
    @Inject private PreferencesService preferencesService;

    public DateEditor(Field field, DateTimeFormatter dateFormatter, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new DateEditorViewModel(field, suggestionProvider, dateFormatter, fieldCheckers, undoManager);

        datePicker.setStringConverter(viewModel.getDateToStringConverter());
        datePicker.getEditor().textProperty().bindBidirectional(viewModel.textProperty());

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), datePicker.getEditor());
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
