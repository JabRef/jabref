package org.jabref.gui.fieldeditors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.formatter.bibtexfields.CleanupURLFormatter;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import java.util.List;

public class UrlEditor extends HBox implements FieldEditorFX {

    @FXML private UrlEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public UrlEditor(String fieldName, DialogService dialogService, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        this.viewModel = new UrlEditorViewModel(fieldName, suggestionProvider, dialogService, fieldCheckers);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        List<MenuItem> contextMenu = EditorMenus.getCleanupURLMenu(textArea);
        textArea.addToContextMenu(contextMenu);
        MenuItem cleanupURLMenuItem = contextMenu.get(0);
        textArea.setOnContextMenuRequested(event ->cleanupURLMenuItem.setDisable("".equals(textArea.getSelectedText())));

        // init paste handler for URLEditor to format pasted url link in textArea
        textArea.setPasteActionHandler(()->{
           textArea.setText(new CleanupURLFormatter().format(textArea.getText()));
        });

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
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
