package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.uithreadaware.UiThreadStringProperty;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import jakarta.inject.Inject;

public class PersonsEditor extends HBox implements FieldEditorFX {

    private final PersonsEditorViewModel viewModel;
    private final TextInputControl textInput;
    private final UiThreadStringProperty decoratedStringProperty;

    @Inject private PreferencesService preferencesService;
    @Inject private UndoManager undoManager;

    public PersonsEditor(final Field field,
                         final SuggestionProvider<?> suggestionProvider,
                         final FieldCheckers fieldCheckers,
                         final boolean isMultiLine) {
        this.viewModel = new PersonsEditorViewModel(field, suggestionProvider, preferencesService.getAutoCompletePreferences(), fieldCheckers, undoManager);

        textInput = isMultiLine ? new EditorTextArea() : new EditorTextField();

        decoratedStringProperty = new UiThreadStringProperty(viewModel.textProperty());
        textInput.textProperty().bindBidirectional(decoratedStringProperty);
        ((ContextMenuAddable) textInput).initContextMenu(EditorMenus.getNameMenu(textInput));
        this.getChildren().add(textInput);

        AutoCompletionTextInputBinding.autoComplete(textInput, viewModel::complete, viewModel.getAutoCompletionConverter(), viewModel.getAutoCompletionStrategy());

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textInput);
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
