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
import jakarta.inject.Inject;

public class CitationKeyEditor extends HBox implements FieldEditorFX {

    @FXML private final CitationKeyEditorViewModel viewModel;
    @FXML private Button generateCitationKeyButton;
    @FXML private EditorTextField textField;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;

    public CitationKeyEditor(Field field,
                             SuggestionProvider<?> suggestionProvider,
                             FieldCheckers fieldCheckers,
                             BibDatabaseContext databaseContext) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new CitationKeyEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferencesService,
                databaseContext,
                undoManager,
                dialogService);

        textField.textProperty().bindBidirectional(viewModel.textProperty());

        textField.initContextMenu(Collections::emptyList);

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public CitationKeyEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);

        // Configure button to generate citation key
        new ActionFactory(preferencesService.getKeyBindingRepository())
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
