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
import org.jabref.gui.fieldeditors.EditorTextField;
import org.jabref.gui.fieldeditors.EditorValidator;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

import static org.jabref.model.entry.field.StandardField.DOI;
import static org.jabref.model.entry.field.StandardField.EPRINT;
import static org.jabref.model.entry.field.StandardField.ISBN;
import static org.jabref.model.entry.field.StandardField.ISSN;

public class IdentifierEditor extends HBox implements FieldEditorFX {

    @FXML private BaseIdentifierEditorViewModel<?> viewModel;
    @FXML private EditorTextField textField;
    @FXML private Button shortenDOIButton;
    @FXML private Button fetchInformationByIdentifierButton;
    @FXML private Button lookupIdentifierButton;

    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private GuiPreferences preferences;
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
                    this.viewModel = new DoiIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
            case ISBN ->
                    this.viewModel = new ISBNIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
            case ISSN ->
                    this.viewModel = new ISSNIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
            case EPRINT ->
                    this.viewModel = new EprintIdentifierEditorViewModel(suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);

            // TODO: Add support for PMID
            case null,
                 default -> {
                assert field != null;
                throw new IllegalStateException("Unable to instantiate a view model for identifier field editor '%s'".formatted(FieldTextMapper.getDisplayName(field)));
            }
        }

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textField.textProperty().bindBidirectional(viewModel.textProperty());

        fetchInformationByIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Get bibliographic data from %0", FieldTextMapper.getDisplayName(field))));
        lookupIdentifierButton.setTooltip(
                new Tooltip(Localization.lang("Look up %0", FieldTextMapper.getDisplayName(field))));
        shortenDOIButton.setTooltip(
                new Tooltip(Localization.lang("Shorten %0", FieldTextMapper.getDisplayName(field))));

        textField.initContextMenu(new DefaultMenu(textField), preferences.getKeyBindingRepository());

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
 
        java.util.stream.Stream.of(fetchInformationByIdentifierButton, lookupIdentifierButton, shortenDOIButton)
            .forEach(button -> {
                button.visibleProperty().bind(viewModel.textProperty().isNotEmpty());
                button.managedProperty().bind(button.visibleProperty());
            });
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

    @FXML
    private void shortenID() {
        viewModel.shortenID();
    }
}
