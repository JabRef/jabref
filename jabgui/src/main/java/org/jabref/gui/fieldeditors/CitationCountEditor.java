package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class CitationCountEditor extends HBox implements FieldEditorFX {
    @FXML private CitationCountEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private Button fetchCitationCountButton;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private StateManager stateManager;
    @Inject private SearchCitationsRelationsService searchCitationsRelationsService;

    public CitationCountEditor(Field field,
                               SuggestionProvider<?> suggestionProvider,
                               FieldCheckers fieldCheckers) {
        Injector.registerExistingAndInject(this);
        this.viewModel = new CitationCountEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                taskExecutor,
                dialogService,
                undoManager,
                stateManager,
                preferences,
                searchCitationsRelationsService);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        textField.textProperty().bindBidirectional(viewModel.textProperty());

        fetchCitationCountButton.setTooltip(
                new Tooltip(Localization.lang("Look up %0", FieldTextMapper.getDisplayName(field))));
        textField.initContextMenu(new DefaultMenu(textField), preferences.getKeyBindingRepository());
        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    @FXML
    private void fetchCitationCount() {
        viewModel.getCitationCount();
    }

    public CitationCountEditorViewModel getViewModel() {
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
}
