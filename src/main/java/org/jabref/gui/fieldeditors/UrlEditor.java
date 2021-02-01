package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.function.Supplier;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class UrlEditor extends HBox implements FieldEditorFX {

    @FXML private final UrlEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public UrlEditor(Field field,
                     DialogService dialogService,
                     SuggestionProvider<?> suggestionProvider,
                     FieldCheckers fieldCheckers,
                     PreferencesService preferences) {
        this.viewModel = new UrlEditorViewModel(field, suggestionProvider, dialogService, fieldCheckers);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        Supplier<List<MenuItem>> contextMenuSupplier = EditorMenus.getCleanupUrlMenu(textArea);
        textArea.initContextMenu(contextMenuSupplier);

        // init paste handler for UrlEditor to format pasted url link in textArea
        textArea.setPasteActionHandler(() -> textArea.setText(new CleanupUrlFormatter().format(new TrimWhitespaceFormatter().format(textArea.getText()))));

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
