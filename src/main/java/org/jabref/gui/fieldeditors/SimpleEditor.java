package org.jabref.gui.fieldeditors;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class SimpleEditor extends HBox implements FieldEditorFX {

    private final SimpleEditorViewModel viewModel;
    private final TextInputControl textInput;

    public SimpleEditor(final Field field,
                        final SuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final JabRefPreferences preferences,
                        final boolean isMultiLine) {
        this.viewModel = new SimpleEditorViewModel(field, suggestionProvider, fieldCheckers);

        textInput = isMultiLine ? new EditorTextArea() : new EditorTextField();
        HBox.setHgrow(textInput, Priority.ALWAYS);

        textInput.textProperty().bindBidirectional(viewModel.textProperty());
        ((ContextMenuAddable) textInput).addToContextMenu(EditorMenus.getDefaultMenu(textInput));
        this.getChildren().add(textInput);

        if (!isMultiLine) {
            AutoCompletionTextInputBinding<?> autoCompleter = AutoCompletionTextInputBinding.autoComplete(textInput, viewModel::complete, viewModel.getAutoCompletionStrategy());
            if (suggestionProvider instanceof ContentSelectorSuggestionProvider) {
                // If content selector values are present, then we want to show the auto complete suggestions immediately on focus
                autoCompleter.setShowOnFocus(true);
            }
        }

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textInput);
    }

    public SimpleEditor(final Field field,
                        final SuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final JabRefPreferences preferences) {
        this(field, suggestionProvider, fieldCheckers, preferences, false);
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
