package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class SimpleEditor extends HBox implements FieldEditorFX {

    @FXML private final SimpleEditorViewModel viewModel;

    public SimpleEditor(final String fieldName,
                        final AutoCompleteSuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final JabRefPreferences preferences,
                        final boolean isSingleLine) {
        this.viewModel = new SimpleEditorViewModel(fieldName, suggestionProvider, fieldCheckers);

        TextInputControl textInput = isSingleLine
                ? new EditorTextField()
                : new EditorTextArea();
        HBox.setHgrow(textInput, Priority.ALWAYS);
        textInput.textProperty().bindBidirectional(viewModel.textProperty());
        ((ContextMenuAddable) textInput).addToContextMenu(EditorMenus.getDefaultMenu(textInput));
        this.getChildren().add(textInput);

        AutoCompletionTextInputBinding<?> autoCompleter = AutoCompletionTextInputBinding.autoComplete(textInput, viewModel::complete, viewModel.getAutoCompletionStrategy());
        if (suggestionProvider instanceof ContentSelectorSuggestionProvider) {
            // If content selector values are present, then we want to show the auto complete suggestions immediately on focus
            autoCompleter.setShowOnFocus(true);
        }

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textInput);
    }


    public SimpleEditor(final String fieldName,
                        final AutoCompleteSuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final JabRefPreferences preferences) {
        this(fieldName, suggestionProvider, fieldCheckers, preferences, false);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
