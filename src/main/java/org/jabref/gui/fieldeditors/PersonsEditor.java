package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class PersonsEditor extends HBox implements FieldEditorFX {

    @FXML private final PersonsEditorViewModel viewModel;

    private TextInputControl textInput;

    public PersonsEditor(final String fieldName,
                         final AutoCompleteSuggestionProvider<?> suggestionProvider,
                         final JabRefPreferences preferences,
                         final FieldCheckers fieldCheckers,
                         final boolean isSingleLine) {
        this.viewModel = new PersonsEditorViewModel(fieldName, suggestionProvider, preferences.getAutoCompletePreferences(), fieldCheckers);

        textInput = isSingleLine
                ? new EditorTextField()
                : new EditorTextArea();
        HBox.setHgrow(textInput, Priority.ALWAYS);
        textInput.textProperty().bindBidirectional(viewModel.textProperty());
        ((ContextMenuAddable) textInput).addToContextMenu(EditorMenus.getNameMenu(textInput));
        this.getChildren().add(textInput);

        AutoCompletionTextInputBinding.autoComplete(textInput, viewModel::complete, viewModel.getAutoCompletionConverter(), viewModel.getAutoCompletionStrategy());

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textInput);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public void requestFocus() {
        textInput.requestFocus();
    }

}
