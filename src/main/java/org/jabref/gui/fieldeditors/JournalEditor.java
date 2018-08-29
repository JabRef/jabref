package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalEditor extends HBox implements FieldEditorFX {

    @FXML private JournalEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public JournalEditor(String fieldName, JournalAbbreviationRepository journalAbbreviationRepository, JabRefPreferences preferences, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new JournalEditorViewModel(fieldName, suggestionProvider, journalAbbreviationRepository, fieldCheckers);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));

        AutoCompletionTextInputBinding.autoComplete(textArea, viewModel::complete);

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
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
}
