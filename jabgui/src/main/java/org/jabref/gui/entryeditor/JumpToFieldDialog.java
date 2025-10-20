package org.jabref.gui.entryeditor;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.textfield.TextFields;

public class JumpToFieldDialog extends BaseDialog<Void> {
    @FXML private TextField searchField;
    private final EntryEditor entryEditor;
    private JumpToFieldViewModel viewModel;

    public JumpToFieldDialog(EntryEditor entryEditor) {
        this.entryEditor = entryEditor;
        this.setTitle(Localization.lang("Jump to field"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                jumpToSelectedField();
            }
            return null;
        });

        Platform.runLater(() -> searchField.requestFocus());
    }

    @FXML
    private void initialize() {
        viewModel = new JumpToFieldViewModel(this.entryEditor);
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());
        TextFields.bindAutoCompletion(searchField, viewModel.getFieldNames());

        searchField.setOnAction(event -> {
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.fire();
            }
            event.consume();
        });
    }

    private void jumpToSelectedField() {
        String selectedField = searchField.getText();

        if (selectedField != null && !selectedField.isEmpty()) {
            String fieldToJumpTo = selectedField.toLowerCase();
            entryEditor.jumpToField(fieldToJumpTo);
        }
    }
}
