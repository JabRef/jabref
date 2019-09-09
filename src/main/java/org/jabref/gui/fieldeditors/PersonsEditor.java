package org.jabref.gui.fieldeditors;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.uithreadaware.UiThreadStringProperty;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class PersonsEditor extends HBox implements FieldEditorFX {

    private final PersonsEditorViewModel viewModel;
    private final TextInputControl textInput;
    private final UiThreadStringProperty decoratedStringProperty;

    public PersonsEditor(final Field field,
                         final AutoCompleteSuggestionProvider<?> suggestionProvider,
                         final JabRefPreferences preferences,
                         final FieldCheckers fieldCheckers,
                         final boolean isSingleLine) {
        this.viewModel = new PersonsEditorViewModel(field, suggestionProvider, preferences.getAutoCompletePreferences(), fieldCheckers);

        textInput = isSingleLine
                ? new EditorTextField()
                : new EditorTextArea();

        decoratedStringProperty = new UiThreadStringProperty(viewModel.textProperty());
        textInput.textProperty().bindBidirectional(decoratedStringProperty);
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
