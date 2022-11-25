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
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CitationKeyEditor extends HBox implements FieldEditorFX {

    private final PreferencesService preferences;
    @FXML private final CitationKeyEditorViewModel viewModel;
    @FXML private Button generateCitationKeyButton;
    @FXML private EditorTextField textField;

    public CitationKeyEditor(Field field,
                             PreferencesService preferences,
                             SuggestionProvider<?> suggestionProvider,
                             FieldCheckers fieldCheckers,
                             BibDatabaseContext databaseContext,
                             UndoManager undoManager,
                             DialogService dialogService) {

        this.preferences = preferences;
        this.viewModel = new CitationKeyEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferences,
                databaseContext,
                undoManager,
                dialogService);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textField.textProperty().bindBidirectional(viewModel.textProperty());

        textField.initContextMenu(Collections::emptyList);

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public CitationKeyEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);

        // Configure button to generate citation key
        new ActionFactory(preferences.getKeyBindingRepository())
                .configureIconButton(
                        StandardActions.GENERATE_CITE_KEY,
                        viewModel.getGenerateCiteKeyCommand(),
                        generateCitationKeyButton);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
