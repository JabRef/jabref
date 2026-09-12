package org.jabref.gui.entryeditor;

import java.util.Locale;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

public class JumpToFieldDialog extends BaseDialog<Void> {
    @FXML private TextField searchField;
    @FXML private Label newFieldHint;
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
                // Closing the dialog restores focus to whatever had it before, which would undo the
                // focus the jump puts on the field. Therefore jump only once the dialog is gone.
                Platform.runLater(this::jumpToSelectedField);
            }
            return null;
        });

        Platform.runLater(() -> searchField.requestFocus());
    }

    @FXML
    private void initialize() {
        viewModel = new JumpToFieldViewModel(this.entryEditor);
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());

        // Prefix matching instead of ControlsFX' default substring matching: the popup always preselects
        // its first suggestion, so "file" would offer (and jump to) "dayfiled" first.
        AutoCompletionBinding<String> autoCompletion = TextFields.bindAutoCompletion(searchField, request -> {
            String userText = request.getUserText().toLowerCase(Locale.ROOT);
            return viewModel.getFieldNames().stream()
                            .filter(fieldName -> fieldName.toLowerCase(Locale.ROOT).startsWith(userText))
                            .toList();
        });
        // The open suggestion popup swallows Enter, so the dialog never sees it: jump on the
        // completion event instead. This also makes clicking a suggestion jump right away.
        autoCompletion.setOnAutoCompleted(_ -> confirm());

        newFieldHint.managedProperty().bind(newFieldHint.visibleProperty());
        newFieldHint.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> viewModel.isNewField(searchField.getText()), searchField.textProperty()));

        searchField.setOnAction(event -> {
            confirm();
            event.consume();
        });
    }

    private void confirm() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.fire();
        }
    }

    private void jumpToSelectedField() {
        String selectedField = searchField.getText();

        if (StringUtil.isNotBlank(selectedField)) {
            String fieldToJumpTo = selectedField.toLowerCase().strip();
            entryEditor.selectField(fieldToJumpTo);
        }
    }
}
