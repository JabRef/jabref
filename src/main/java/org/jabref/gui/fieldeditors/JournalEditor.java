package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.entry.BibEntry;

public class JournalEditor extends HBox implements FieldEditorFX {

    @FXML private JournalEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    private Optional<BibEntry> entry;

    public JournalEditor(String fieldName, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, ContentAutoCompleters autoCompleter) {
        this.viewModel = new JournalEditorViewModel(fieldName, autoCompleter, journalAbbreviationLoader, journalAbbreviationPreferences);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));
    }

    public JournalEditorViewModel getViewModel() {
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

    @FXML
    private void toggleAbbreviation(ActionEvent event) {
        viewModel.toggleAbbreviation();
    }
}
