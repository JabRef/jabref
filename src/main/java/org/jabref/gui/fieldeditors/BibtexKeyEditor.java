package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class BibtexKeyEditor extends HBox implements FieldEditorFX {

    private final JabRefPreferences preferences;
    @FXML private BibtexKeyEditorViewModel viewModel;
    @FXML private Button generateCiteKeyButton;
    @FXML private EditorTextArea textArea;

    public BibtexKeyEditor(String fieldName, JabRefPreferences preferences, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, BibDatabaseContext databaseContext, UndoManager undoManager, DialogService dialogService) {
        this.preferences = preferences;
        this.viewModel = new BibtexKeyEditorViewModel(fieldName, suggestionProvider, fieldCheckers, preferences.getEntryEditorPreferences(), databaseContext, undoManager, dialogService);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public BibtexKeyEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);

        // Configure cite key button
        new ActionFactory(preferences.getKeyBindingRepository())
                .configureIconButton(
                        StandardActions.GENERATE_CITE_KEY,
                        viewModel.getGenerateCiteKeyCommand(),
                        generateCiteKeyButton);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
