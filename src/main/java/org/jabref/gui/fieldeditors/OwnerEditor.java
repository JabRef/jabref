package org.jabref.gui.fieldeditors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditor extends HBox implements FieldEditorFX {

    @FXML private OwnerEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public OwnerEditor(String fieldName, JabRefPreferences preferences, ContentAutoCompleters autoCompleter) {
        this.viewModel = new OwnerEditorViewModel(fieldName, autoCompleter, preferences);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
    }

    public OwnerEditorViewModel getViewModel() {
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
    private void setOwner(ActionEvent event) {
        viewModel.setOwner();
    }
}
