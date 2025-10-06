package org.jabref.gui.edit.automaticfiededitor.clearfieldcontent;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

import static org.jabref.gui.util.FieldsUtil.FIELD_STRING_CONVERTER;

public class ClearFieldContentTabView extends AbstractAutomaticFieldEditorTabView {
    public Button clearFieldButton;

    @FXML
    private ComboBox<Field> fieldComboBox;

    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;

    private ClearFieldContentViewModel viewModel;

    private final StateManager stateManager;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public ClearFieldContentTabView(BibDatabase database, StateManager stateManager) {
        this.selectedEntries = stateManager.getSelectedEntries();
        this.database = database;
        this.stateManager = stateManager;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new ClearFieldContentViewModel(database, selectedEntries, stateManager);
        fieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        fieldComboBox.getItems().setAll(viewModel.getSetFields());

        fieldComboBox.valueProperty().bindBidirectional(viewModel.selectedFieldProperty());
        EasyBind.listen(fieldComboBox.getEditor().textProperty(), observable -> fieldComboBox.commitValue());

        Platform.runLater(() -> visualizer.initVisualization(viewModel.fieldValidationStatus(), fieldComboBox, true));
    }

    @Override
    public String getTabName() {
        return Localization.lang("Clear content");
    }

    @FXML
    void clearField() {
        viewModel.clearSelectedField();
    }
}
