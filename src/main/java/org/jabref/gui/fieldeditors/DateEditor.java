package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class DateEditor extends HBox implements FieldEditorFX {

    @FXML private DateEditorViewModel viewModel;
    @FXML private TemporalAccessorPicker datePicker;

    public DateEditor(Field field, DateTimeFormatter dateFormatter, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new DateEditorViewModel(field, suggestionProvider, dateFormatter, fieldCheckers);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        datePicker.setStringConverter(viewModel.getDateToStringConverter());
        datePicker.getEditor().textProperty().bindBidirectional(viewModel.textProperty());
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
