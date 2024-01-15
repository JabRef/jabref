package org.jabref.gui.fieldeditors.identifier;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.EditorTextArea;
import org.jabref.gui.fieldeditors.EditorValidator;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

import static org.jabref.model.entry.field.StandardField.DOI;
import static org.jabref.model.entry.field.StandardField.EPRINT;
import static org.jabref.model.entry.field.StandardField.ISBN;

public class IdentifierEditor extends HBox implements FieldEditorFX {

    @FXML private BaseIdentifierEditorViewModel<?> viewModel;
    @FXML private EditorTextArea textArea;
    @FXML private Button fetchInformationByIdentifierButton;
    @FXML private Button lookupIdentifierButton;

    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;
    @Inject private UndoManager undoManager;
    @Inject private StateManager stateManager;

    private Optional<BibEntry> entry = Optional.empty();

    public IdentifierEditor(Field field,
                            SuggestionProvider<?> suggestionProvider,
                            FieldCheckers fieldCheckers) {

        // Viewloader must be called after the viewmodel is loaded,
        // but we need the injected vars to create the viewmodels.
        Injector.registerExistingAndInject(this);

        switch (field) {
            case DOI ->
                    this.viewModel = new DoiIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferencesService, undoManager, stateManager);
            case ISBN ->
                    this.viewModel = new ISBNIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferencesService, undoManager, stateManager);
            case EPRINT ->
                    this.viewModel = new EprintIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferencesService, undoManager);

            case null, default -> {
                assert field != null;
                throw new IllegalStateException("Unable to instantiate a view model for identifier field editor '%s'".formatted(field.getDisplayName()));
            }
        }

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        fetchInformationByIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Get bibliographic data from %0", field.getDisplayName())));
        lookupIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Look up %0", field.getDisplayName())));

        if (field.equals(DOI)) {
            textArea.initContextMenu(EditorMenus.getDOIMenu(textArea, dialogService, preferencesService));
        } else {
            textArea.initContextMenu(new DefaultMenu(textArea));
        }

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textArea);
    }

    public BaseIdentifierEditorViewModel<?> getViewModel() {
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
    private void fetchInformationByIdentifier() {
        entry.ifPresent(viewModel::fetchBibliographyInformation);
    }

    @FXML
    private void lookupIdentifier() {
        entry.ifPresent(viewModel::lookupIdentifier);
    }

    @FXML
    private void openExternalLink() {
        viewModel.openExternalLink();
    }
}
