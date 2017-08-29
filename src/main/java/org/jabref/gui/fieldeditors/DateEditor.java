package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;

public class DateEditor extends HBox implements FieldEditorFX {

    @FXML private DateEditorViewModel viewModel;
    @FXML private TemporalAccessorPicker datePicker;

    public DateEditor(String fieldName, DateTimeFormatter dateFormatter, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new DateEditorViewModel(fieldName, suggestionProvider, dateFormatter, fieldCheckers);

        ControlHelper.loadFXMLForControl(this);

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
