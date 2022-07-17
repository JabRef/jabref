package org.jabref.gui.edit.automaticfiededitor.editfieldcontent;

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
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.views.ViewLoader;

public class EditFieldContentTabView extends AbstractAutomaticFieldEditorTabView {
    public Button appendValueButton;
    @FXML
    private ComboBox<Field> fieldComboBox;

    @FXML
    private TextField fieldValueTextField;

    @FXML
    private CheckBox overwriteFieldContentCheckBox;

    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;

    private EditFieldContentViewModel viewModel;

    private final NamedCompound dialogEdits;

    public EditFieldContentTabView(List<BibEntry> selectedEntries, BibDatabase database, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.database = database;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new EditFieldContentViewModel(database, selectedEntries, dialogEdits);
        fieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field == null ? "" : field.getDisplayName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        fieldComboBox.getItems().setAll(viewModel.getAllFields());

        fieldComboBox.getSelectionModel().selectFirst();

        viewModel.selectedFieldProperty().bindBidirectional(fieldComboBox.valueProperty());

        viewModel.fieldValueProperty().bindBidirectional(fieldValueTextField.textProperty());

        viewModel.overwriteFieldContentProperty().bindBidirectional(overwriteFieldContentCheckBox.selectedProperty());

        appendValueButton.disableProperty().bind(overwriteFieldContentCheckBox.selectedProperty().not());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Edit content");
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
