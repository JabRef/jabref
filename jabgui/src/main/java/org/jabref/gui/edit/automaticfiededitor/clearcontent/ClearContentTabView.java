package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class ClearContentTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {

    private final ComboBox<Field> fieldCombo;
    private final CheckBox showOnlySetFields;
    private final Button clearButton;
    private final ClearContentViewModel viewModel;

    public ClearContentTabView(StateManager stateManager) {
        super();

        this.viewModel = new ClearContentViewModel(stateManager);

        Label fieldLabel = new Label(Localization.lang("Field"));
        fieldCombo = new ComboBox<>();
        showOnlySetFields = new CheckBox(Localization.lang("Show only set fields"));
        clearButton = new Button(Localization.lang("Clear field content"));

        // populate initially with all fields
        fieldCombo.getItems().setAll(
                viewModel.getAllFields().stream().toList()
        );

        // toggle filtering when checkbox is clicked
        showOnlySetFields.setOnAction(e -> {
            if (showOnlySetFields.isSelected()) {
                fieldCombo.getItems().setAll(
                        viewModel.getSetFieldsOnly().stream().toList()
                );
            } else {
                fieldCombo.getItems().setAll(
                        viewModel.getAllFields().stream().toList()
                );
            }
        });

        VBox box = new VBox(10);

        Label title = new Label(Localization.lang("Clear field content for selected entries"));
        title.getStyleClass().add("sectionHeader");          // match Edit tab styling

        HBox row = new HBox(10, fieldLabel, fieldCombo, showOnlySetFields);
        box.getChildren().addAll(title, row, clearButton);
        this.getChildren().add(box);

        clearButton.setOnAction(e -> {
            Field chosen = fieldCombo.getValue();   // keep ComboBox<Field>
            if (chosen != null) {
                viewModel.clearField(chosen);       // VM builds/publishes the undo compound
                System.out.println("Cleared field: " + chosen.getName());
            }
        });
    }

    @Override
    public String getTabName() {
        return Localization.lang("Clear content");
    }
}
