package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.CitationCountIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationCountEditorViewModel extends BaseIdentifierEditorViewModel<CitationCountIdentifier> {
    public static final Logger LOGGER = LoggerFactory.getLogger(DoiIdentifierEditorViewModel.class);

    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final SearchCitationsRelationsService searchCitationsRelationsService;

    public CitationCountEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                        FieldCheckers fieldCheckers,
                                        DialogService dialogService,
                                        TaskExecutor taskExecutor,
                                        GuiPreferences preferences,
                                        UndoManager undoManager,
                                        StateManager stateManager,
                                        SearchCitationsRelationsService searchCitationsRelationsService){

        super(StandardField.CITATIONCOUNT, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager);
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.searchCitationsRelationsService = searchCitationsRelationsService;
        configure(false,true);
    }
    @Override
    public void lookupIdentifier(BibEntry bibEntry) {

        BackgroundTask.wrap(() -> searchCitationsRelationsService.getCitationCount(bibEntry))
                      .onRunning(() -> identifierLookupInProgress.setValue(true))
                      .onFinished(() -> identifierLookupInProgress.setValue(false))
                      .onSuccess(identifier -> {
                              entry.setField(field, String.valueOf(identifier));
                      }).executeWith(taskExecutor);
    }

}
