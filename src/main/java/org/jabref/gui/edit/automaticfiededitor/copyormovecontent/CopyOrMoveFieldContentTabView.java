package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

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

public class CopyOrMoveFieldContentTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    public Button copyContentButton;
    @FXML
    private Button moveContentButton;

    @FXML
    private Button swapContentButton;

    @FXML
    private ComboBox<Field> fromFieldComboBox;
    @FXML
    private ComboBox<Field> toFieldComboBox;

    @FXML
    private CheckBox overwriteFieldContentCheckBox;

    private CopyOrMoveFieldContentTabViewModel viewModel;
    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;
    private final StateManager stateManager;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public CopyOrMoveFieldContentTabView(BibDatabase database, StateManager stateManager) {
        this.selectedEntries = new ArrayList<>(stateManager.getSelectedEntries());
        this.database = database;
        this.stateManager = stateManager;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new CopyOrMoveFieldContentTabViewModel(selectedEntries, database, stateManager);
        initializeFromAndToComboBox();

        viewModel.overwriteFieldContentProperty().bindBidirectional(overwriteFieldContentCheckBox.selectedProperty());

        moveContentButton.disableProperty().bind(viewModel.canMoveProperty().not());
        swapContentButton.disableProperty().bind(viewModel.canSwapProperty().not());
        copyContentButton.disableProperty().bind(viewModel.toFieldValidationStatus().validProperty().not());
        overwriteFieldContentCheckBox.disableProperty().bind(viewModel.toFieldValidationStatus().validProperty().not());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.toFieldValidationStatus(), toFieldComboBox, true);
        });
    }

    private void initializeFromAndToComboBox() {
        fromFieldComboBox.getItems().setAll(viewModel.getAllFields());
        toFieldComboBox.getItems().setAll(viewModel.getAllFields());

        fromFieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        toFieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        fromFieldComboBox.valueProperty().bindBidirectional(viewModel.fromFieldProperty());
        toFieldComboBox.valueProperty().bindBidirectional(viewModel.toFieldProperty());

        EasyBind.listen(fromFieldComboBox.getEditor().textProperty(), observable -> fromFieldComboBox.commitValue());
        EasyBind.listen(toFieldComboBox.getEditor().textProperty(), observable -> toFieldComboBox.commitValue());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Copy or Move content");
    }

    @FXML
    void copyContent() {
        viewModel.copyValue();
    }

    @FXML
    void moveContent() {
        viewModel.moveValue();
    }

    @FXML
    void swapContent() {
        viewModel.swapValues();
    }
}
