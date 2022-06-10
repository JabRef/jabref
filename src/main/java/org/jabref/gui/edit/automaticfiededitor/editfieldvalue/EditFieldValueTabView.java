package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.views.ViewLoader;

public class EditFieldValueTabView extends AbstractAutomaticFieldEditorTabView {
    @FXML
    private ComboBox<Field> fieldComboBox;

    @FXML
    private TextField fieldValueTextField;

    @FXML
    private CheckBox overwriteNonEmptyFieldsCheckBox;

    @Inject private UndoManager undoManager;

    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;

    private EditFieldValueViewModel viewModel;

    private NamedCompound edits;

    public EditFieldValueTabView(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound edits) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.edits = edits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new EditFieldValueViewModel(databaseContext, selectedEntries, edits);
        fieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });
        fieldComboBox.getItems().addAll(viewModel.getAllFields());
        viewModel.selectedFieldProperty().bindBidirectional(fieldComboBox.valueProperty());
        viewModel.fieldValueProperty().bindBidirectional(fieldValueTextField.textProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Edit field value");
    }

    @FXML
    void appendToFieldValue() {
        viewModel.appendToFieldValue();
    }

    @FXML
    void clearField() {
        viewModel.clearSelectedField();
    }

    @FXML
    void setFieldValue() {
        viewModel.setFieldValue();
    }
}
