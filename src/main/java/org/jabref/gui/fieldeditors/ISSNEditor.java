package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class ISSNEditor extends HBox implements FieldEditorFX {
    @FXML private ISSNEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    @FXML private Button journalInfoButton;
    @FXML private Button fetchInformationByIdentifierButton;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private StateManager stateManager;
    private Optional<BibEntry> entry = Optional.empty();

    public ISSNEditor(Field field,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new ISSNEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                taskExecutor,
                dialogService,
                undoManager,
                stateManager,
                preferencesService);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.initContextMenu(new DefaultMenu(textArea));

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public ISSNEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = Optional.of(entry);
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public void requestFocus() {
        textArea.requestFocus();
    }

    @FXML
    private void fetchInformationByIdentifier() {
        entry.ifPresent(viewModel::fetchBibliographyInformation);
    }

    @FXML
    private void showJournalInfo() {
        if (JournalInfoOptInDialogHelper.isJournalInfoEnabled(dialogService, preferencesService.getEntryEditorPreferences())) {
            viewModel.showJournalInfo(journalInfoButton);
        }
    }
}
