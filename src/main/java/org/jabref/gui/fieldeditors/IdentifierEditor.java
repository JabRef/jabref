package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class IdentifierEditor extends HBox implements FieldEditorFX {

    @FXML private IdentifierEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    @FXML private Button fetchInformationByIdentifierButton;
    @FXML private Button lookupIdentifierButton;
    private Optional<BibEntry> entry;

    public IdentifierEditor(Field field, TaskExecutor taskExecutor, DialogService dialogService, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        this.viewModel = new IdentifierEditorViewModel(field, suggestionProvider, taskExecutor, dialogService, fieldCheckers);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        fetchInformationByIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Get BibTeX data from %0", field.getDisplayName())));
        lookupIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Look up %0", field.getDisplayName())));

        if (field.equals(StandardField.DOI)) {
            textArea.addToContextMenu(EditorMenus.getDOIMenu(textArea));
        } else {
            textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));
        }

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public IdentifierEditorViewModel getViewModel() {
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
    private void fetchInformationByIdentifier(ActionEvent event) {
        entry.ifPresent(bibEntry -> viewModel.fetchInformationByIdentifier(bibEntry));
    }

    @FXML
    private void lookupIdentifier(ActionEvent event) {
        entry.ifPresent(bibEntry -> viewModel.lookupIdentifier(bibEntry));
    }

    @FXML
    private void openExternalLink(ActionEvent event) {
        viewModel.openExternalLink();
    }
}
