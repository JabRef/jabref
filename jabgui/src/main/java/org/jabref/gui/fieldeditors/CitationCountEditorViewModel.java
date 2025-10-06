package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationCountEditorViewModel extends AbstractEditorViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitationCountEditorViewModel.class);

    protected final BooleanProperty fetchCitationCountInProgress = new SimpleBooleanProperty(false);
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final SearchCitationsRelationsService searchCitationsRelationsService;

    public CitationCountEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            UndoManager undoManager,
            StateManager stateManager,
            GuiPreferences preferences,
            SearchCitationsRelationsService searchCitationsRelationsService) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.searchCitationsRelationsService = searchCitationsRelationsService;
    }

    public BooleanProperty fetchCitationCountInProgressProperty() {
        return fetchCitationCountInProgress;
    }

    public boolean getFetchCitationCountInProgress() {
        return fetchCitationCountInProgress.get();
    }

    public void getCitationCount() {
        Optional<String> fieldContent = entry.getField(field);
        BackgroundTask.wrap(() -> searchCitationsRelationsService.getCitationCount(this.entry, fieldContent))
                      .onRunning(() -> fetchCitationCountInProgress.setValue(true))
                      .onFinished(() -> fetchCitationCountInProgress.setValue(false))
                      .onFailure(e -> {
                          dialogService.notify(Localization.lang("Error occurred when getting citation count, please try again or check the identifier.\n\n%0", e.getLocalizedMessage()));
                          LOGGER.error("Error while fetching citation count", e);
                      })
                      .onSuccess(identifier -> {
                          entry.setField(field, String.valueOf(identifier));
                      }).executeWith(taskExecutor);
    }
}
