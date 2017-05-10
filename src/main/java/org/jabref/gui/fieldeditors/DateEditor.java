package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.model.entry.BibEntry;

public class DateEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private DateEditorViewModel viewModel;
    @FXML private TemporalAccessorPicker datePicker;

    public DateEditor(String fieldName, DateTimeFormatter dateFormatter) {
        this.fieldName = fieldName;
        this.viewModel = new DateEditorViewModel(dateFormatter);

        ControlHelper.loadFXMLForControl(this);

        datePicker.setStringConverter(viewModel.getDateToStringConverter());
        datePicker.getEditor().textProperty().bindBidirectional(viewModel.textProperty());
    }

    public DateEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(fieldName, entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
