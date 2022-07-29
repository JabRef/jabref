package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

import static org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FIELD_STRING_CONVERTER;

public class RenameFieldTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    @FXML
    private Button renameButton;
    @FXML
    private ComboBox<Field> fieldComboBox;
    @FXML
    private TextField newFieldNameTextField;
    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;
    private final StateManager stateManager;
    private RenameFieldViewModel viewModel;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public RenameFieldTabView(List<BibEntry> selectedEntries, BibDatabase database, StateManager stateManager) {
        this.selectedEntries = selectedEntries;
        this.database = database;
        this.stateManager = stateManager;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new RenameFieldViewModel(selectedEntries, database, stateManager);

        fieldComboBox.getItems().setAll(viewModel.getAllFields());
        fieldComboBox.getSelectionModel().selectFirst();

        fieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        fieldComboBox.valueProperty().bindBidirectional(viewModel.selectedFieldProperty());
        EasyBind.listen(fieldComboBox.getEditor().textProperty(), observable -> fieldComboBox.commitValue());

        renameButton.disableProperty().bind(viewModel.canRenameProperty().not());

        newFieldNameTextField.textProperty().bindBidirectional(viewModel.newFieldNameProperty());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.fieldNameValidationStatus(), newFieldNameTextField, true);
        });
    }

    @Override
    public String getTabName() {
        return Localization.lang("Rename field");
    }

    @FXML
    void renameField() {
        viewModel.renameField();
    }
}
