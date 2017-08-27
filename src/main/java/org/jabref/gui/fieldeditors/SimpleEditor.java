package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
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

    public SimpleEditor(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        this.viewModel = new SimpleEditorViewModel(fieldName, suggestionProvider, fieldCheckers);

        EditorTextArea textArea = new EditorTextArea();
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));
        this.getChildren().add(textArea);

        AutoCompletionTextInputBinding<?> autoCompleter = AutoCompletionTextInputBinding.autoComplete(textArea, viewModel::complete, viewModel.getAutoCompletionStrategy());
        if (suggestionProvider instanceof ContentSelectorSuggestionProvider) {
            // If content selector values are present, then we want to show the auto complete suggestions immediately on focus
            autoCompleter.setShowOnFocus(true);
        }

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
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
