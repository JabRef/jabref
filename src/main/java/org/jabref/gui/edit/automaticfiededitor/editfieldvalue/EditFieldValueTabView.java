package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.Comparator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    public Button appendValueButton;
    @FXML
    private ComboBox<Field> fieldComboBox;

    @FXML
    private TextField fieldValueTextField;

    @FXML
    private CheckBox overwriteNonEmptyFieldsCheckBox;

    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;

    private EditFieldValueViewModel viewModel;

    private final NamedCompound dialogEdits;

    public EditFieldValueTabView(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new EditFieldValueViewModel(databaseContext, selectedEntries, dialogEdits);
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
        fieldComboBox.getItems().addAll(viewModel.getAllFields().sorted(Comparator.comparing(Field::getName)));
        fieldComboBox.getSelectionModel().selectFirst();
        viewModel.selectedFieldProperty().bindBidirectional(fieldComboBox.valueProperty());

        viewModel.fieldValueProperty().bindBidirectional(fieldValueTextField.textProperty());

        viewModel.overwriteNonEmptyFieldsProperty().bindBidirectional(overwriteNonEmptyFieldsCheckBox.selectedProperty());

        appendValueButton.disableProperty().bind(overwriteNonEmptyFieldsCheckBox.selectedProperty().not());
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
