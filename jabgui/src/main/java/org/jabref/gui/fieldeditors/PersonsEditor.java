package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.uithreadaware.UiThreadStringProperty;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;

public class PersonsEditor extends HBox implements FieldEditorFX {

    private final PersonsEditorViewModel viewModel;
    private final TextInputControl textInput;
    private final UiThreadStringProperty decoratedStringProperty;

    public PersonsEditor(final Field field,
                         final SuggestionProvider<?> suggestionProvider,
                         final FieldCheckers fieldCheckers,
                         final boolean isMultiLine,
                         final UndoManager undoManager,
                         UndoAction undoAction,
                         RedoAction redoAction) {
        GuiPreferences preferences = Injector.instantiateModelOrService(GuiPreferences.class);
        KeyBindingRepository keyBindingRepository = preferences.getKeyBindingRepository();

        this.viewModel = new PersonsEditorViewModel(field, suggestionProvider, preferences.getAutoCompletePreferences(), fieldCheckers, undoManager);
        textInput = isMultiLine ? new EditorTextArea() : new EditorTextField();
        decoratedStringProperty = new UiThreadStringProperty(viewModel.textProperty());
        establishBinding(textInput, decoratedStringProperty, keyBindingRepository, undoAction, redoAction);
        ((ContextMenuAddable) textInput).initContextMenu(EditorMenus.getNameMenu(textInput), keyBindingRepository);
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
