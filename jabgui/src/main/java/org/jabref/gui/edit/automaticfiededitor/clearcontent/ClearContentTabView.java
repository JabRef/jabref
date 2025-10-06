package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

import static org.jabref.gui.util.FieldsUtil.FIELD_STRING_CONVERTER;

public class ClearContentTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {

    @FXML private ComboBox<Field> fieldComboBox;
    @FXML private CheckBox showOnlySetFieldsCheckBox;
    @FXML private Button clearButton;
    private final StateManager stateManager;
    private ClearContentViewModel viewModel;

    public ClearContentTabView(StateManager stateManager) {
        this.stateManager = stateManager;
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new ClearContentViewModel(stateManager);

        fieldComboBox.setConverter(FIELD_STRING_CONVERTER);

        fieldComboBox.getItems().setAll(viewModel.getAllFields());

        if (!fieldComboBox.getItems().isEmpty()) {
            fieldComboBox.getSelectionModel().selectFirst();
        }
        showOnlySetFieldsCheckBox.setOnAction(e -> {
            var items = showOnlySetFieldsCheckBox.isSelected()
                        ? viewModel.getSetFieldsOnly()
                        : viewModel.getAllFields();
            fieldComboBox.getItems().setAll(items);
            if (!items.isEmpty()) {
                fieldComboBox.getSelectionModel().selectFirst();
            }
        });

        clearButton.disableProperty().bind(fieldComboBox.valueProperty().isNull());

        Platform.runLater(fieldComboBox::requestFocus);
    }

    @FXML
    private void onClear() {
        Field chosen = fieldComboBox.getValue();
        if (chosen != null) {
            viewModel.clearField(chosen);
        }
    }

    @Override
    public String getTabName() {
        return Localization.lang("Clear content");
    }
}
