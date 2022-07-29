package org.jabref.gui.edit.automaticfiededitor.editfieldcontent;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

import static org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FIELD_STRING_CONVERTER;

public class EditFieldContentTabView extends AbstractAutomaticFieldEditorTabView {
    public Button appendValueButton;
    public Button clearFieldButton;
    public Button setValueButton;
    @FXML
    private ComboBox<Field> fieldComboBox;

    @FXML
    private TextField fieldValueTextField;

    @FXML
    private CheckBox overwriteFieldContentCheckBox;

    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;

    private EditFieldContentViewModel viewModel;

    private final StateManager stateManager;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public EditFieldContentTabView(BibDatabase database, StateManager stateManager) {
        this.selectedEntries = stateManager.getSelectedEntries();
        this.database = database;
        this.stateManager = stateManager;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new EditFieldContentViewModel(database, selectedEntries, stateManager);
        fieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        fieldComboBox.getItems().setAll(viewModel.getAllFields());

        fieldComboBox.getSelectionModel().selectFirst();

        fieldComboBox.valueProperty().bindBidirectional(viewModel.selectedFieldProperty());
        EasyBind.listen(fieldComboBox.getEditor().textProperty(), observable -> fieldComboBox.commitValue());

        fieldValueTextField.textProperty().bindBidirectional(viewModel.fieldValueProperty());

        overwriteFieldContentCheckBox.selectedProperty().bindBidirectional(viewModel.overwriteFieldContentProperty());

        appendValueButton.disableProperty().bind(viewModel.canAppendProperty().not());
        setValueButton.disableProperty().bind(viewModel.fieldValidationStatus().validProperty().not());
        clearFieldButton.disableProperty().bind(viewModel.fieldValidationStatus().validProperty().not());
        overwriteFieldContentCheckBox.disableProperty().bind(viewModel.fieldValidationStatus().validProperty().not());

        Platform.runLater(() -> visualizer.initVisualization(viewModel.fieldValidationStatus(), fieldComboBox, true));
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
