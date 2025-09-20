package org.jabref.gui.fieldeditors;

import java.util.Collections;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class CitationKeyEditor extends HBox implements FieldEditorFX {

    @FXML private final CitationKeyEditorViewModel viewModel;
    @FXML private Button generateCitationKeyButton;
    @FXML private EditorTextField textField;

    @Inject private GuiPreferences preferences;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;

    public CitationKeyEditor(Field field,
                             SuggestionProvider<?> suggestionProvider,
                             FieldCheckers fieldCheckers,
                             BibDatabaseContext databaseContext,
                             UndoAction undoAction,
                             RedoAction redoAction) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new CitationKeyEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferences,
                databaseContext,
                undoManager,
                dialogService);

        textField.setId(field.getName());

        establishBinding(textField, viewModel.textProperty(), keyBindingRepository, undoAction, redoAction);
        textField.initContextMenu(Collections::emptyList, keyBindingRepository);
        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public CitationKeyEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);

        // Configure button to generate citation key
        new ActionFactory().configureIconButton(
                StandardActions.GENERATE_CITE_KEY,
                viewModel.getGenerateCiteKeyCommand(),
                generateCitationKeyButton);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
