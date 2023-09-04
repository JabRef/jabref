package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalEditor extends HBox implements FieldEditorFX {

    @FXML private JournalEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private Button journalInfoButton;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public JournalEditor(Field field,
                         TaskExecutor taskExecutor,
                         DialogService dialogService,
                         JournalAbbreviationRepository journalAbbreviationRepository,
                         PreferencesService preferences,
                         SuggestionProvider<?> suggestionProvider,
                         FieldCheckers fieldCheckers) {
        this.dialogService = dialogService;
        this.preferencesService = preferences;

        this.viewModel = new JournalEditorViewModel(
                field,
                suggestionProvider,
                journalAbbreviationRepository,
                fieldCheckers,
                taskExecutor,
                dialogService);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textField.textProperty().bindBidirectional(viewModel.textProperty());
        textField.initContextMenu(new DefaultMenu(textField));

        AutoCompletionTextInputBinding.autoComplete(textField, viewModel::complete);

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public JournalEditorViewModel getViewModel() {
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

    @FXML
    private void toggleAbbreviation() {
        viewModel.toggleAbbreviation();
    }

    @FXML
    private void showJournalInfo() {
        if (JournalInfoOptInDialogHelper.isJournalInfoEnabled(dialogService, preferencesService.getEntryEditorPreferences())) {
            viewModel.showJournalInfo(journalInfoButton);
        }
    }
}
