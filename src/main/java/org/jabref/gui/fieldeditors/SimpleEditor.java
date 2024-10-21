package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class SimpleEditor extends HBox implements FieldEditorFX {

    private final SimpleEditorViewModel viewModel;
    private final TextInputControl textInput;
    private final boolean isMultiLine;

    public SimpleEditor(final Field field,
                        final SuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final GuiPreferences preferences,
                        final boolean isMultiLine,
                        final UndoManager undoManager,
                        UndoAction undoAction,
                        RedoAction redoAction) {
        this.viewModel = new SimpleEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager);
        this.isMultiLine = isMultiLine;

        textInput = createTextInputControl();
        HBox.setHgrow(textInput, Priority.ALWAYS);

        establishBinding(textInput, viewModel.textProperty(), preferences.getKeyBindingRepository(), undoAction, redoAction);

        ((ContextMenuAddable) textInput).initContextMenu(new DefaultMenu(textInput), preferences.getKeyBindingRepository());
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

    protected TextInputControl createTextInputControl() {
        return isMultiLine ? new EditorTextArea() : new EditorTextField();
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

    protected TextInputControl getTextInput() {
        return textInput;
    }
}
