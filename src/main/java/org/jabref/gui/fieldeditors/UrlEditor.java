package org.jabref.gui.fieldeditors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.model.entry.BibEntry;

public class UrlEditor extends HBox implements FieldEditorFX {

    @FXML private UrlEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public UrlEditor(String fieldName, DialogService dialogService, ContentAutoCompleters autoCompleter) {
        this.viewModel = new UrlEditorViewModel(fieldName, autoCompleter, dialogService);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
    }

    public UrlEditorViewModel getViewModel() {
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
    private void openExternalLink(ActionEvent event) {
        viewModel.openExternalLink();
    }
}
