package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class OwnerEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private OwnerEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    private Optional<BibEntry> entry;

    public OwnerEditor(String fieldName, JabRefPreferences preferences) {
        this.fieldName = fieldName;
        this.viewModel = new OwnerEditorViewModel(preferences);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
    }

    public OwnerEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = Optional.of(entry);
        viewModel.bindToEntry(fieldName, entry);
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
