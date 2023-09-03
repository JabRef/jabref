package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ISSNEditor extends HBox implements FieldEditorFX {
    @FXML private ISSNEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    @FXML private Button journalInfoButton;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public ISSNEditor(Field field,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers,
                      PreferencesService preferences,
                      TaskExecutor taskExecutor,
                      DialogService dialogService) {
        this.preferencesService = preferences;
        this.dialogService = dialogService;

        this.viewModel = new ISSNEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                taskExecutor,
                dialogService);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.initContextMenu(new DefaultMenu(textArea));

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public ISSNEditorViewModel getViewModel() {
        return viewModel;
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
        textArea.requestFocus();
    }

    @FXML
    private void showJournalInfo() {
        if (JournalInfoOptInDialogHelper.isJournalInfoEnabled(dialogService, preferencesService.getEntryEditorPreferences())) {
            viewModel.showJournalInfo(journalInfoButton);
        }
    }
}
