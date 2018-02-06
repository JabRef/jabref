package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyEditor extends HBox implements FieldEditorFX {

    @FXML private BibtexKeyEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public BibtexKeyEditor(String fieldName, JabRefPreferences preferences, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, BibtexKeyPatternPreferences keyPatternPreferences, BibDatabaseContext bibDatabaseContext, UndoManager undoManager) {
        this.viewModel = new BibtexKeyEditorViewModel(fieldName, suggestionProvider, fieldCheckers, keyPatternPreferences, bibDatabaseContext, undoManager);

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public BibtexKeyEditorViewModel getViewModel() {
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
    private void generateKey(ActionEvent event) {
        viewModel.generateKey();
    }
}
