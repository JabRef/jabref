package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class OwnerEditor extends HBox implements FieldEditorFX {

    @FXML private OwnerEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    @Inject private PreferencesService preferencesService;
    @Inject private UndoManager undoManager;

    public OwnerEditor(Field field,
                       SuggestionProvider<?> suggestionProvider,
                       FieldCheckers fieldCheckers) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new OwnerEditorViewModel(field, suggestionProvider, preferencesService, fieldCheckers, undoManager);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.initContextMenu(EditorMenus.getNameMenu(textArea));

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
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
    private void setOwner() {
        viewModel.setOwner();
    }
}
