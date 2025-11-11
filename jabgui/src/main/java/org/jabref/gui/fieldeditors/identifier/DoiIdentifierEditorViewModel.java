package org.jabref.gui.fieldeditors.identifier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<DOI> {
    public static final Logger LOGGER = LoggerFactory.getLogger(DoiIdentifierEditorViewModel.class);

    private final UndoManager undoManager;
    private final StateManager stateManager;

    public DoiIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                        FieldCheckers fieldCheckers,
                                        DialogService dialogService,
                                        TaskExecutor taskExecutor,
                                        GuiPreferences preferences,
                                        UndoManager undoManager,
                                        StateManager stateManager) {
        super(StandardField.DOI, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager);
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        configure(true, true);
    }

    @Override
    public void lookupIdentifier(BibEntry bibEntry) {
        CrossRef doiFetcher = new CrossRef();

        BackgroundTask.wrap(() -> doiFetcher.findIdentifier(entry))
            .onRunning(() -> identifierLookupInProgress.setValue(true))
            .onFinished(() -> identifierLookupInProgress.setValue(false))
            .onSuccess(identifier -> {
                if (identifier.isPresent()) {
                    entry.setField(field, identifier.get().asString());
                } else {
                    dialogService.notify(Localization.lang("No %0 found", field.getDisplayName()));
                }
            }).onFailure(e -> handleIdentifierFetchingError(e, doiFetcher)).executeWith(taskExecutor);
    }

    @Override
    public void fetchBibliographyInformation(BibEntry bibEntry) {
        stateManager.getActiveDatabase().ifPresentOrElse(
                databaseContext -> new FetchAndMergeEntry(databaseContext, taskExecutor, preferences, dialogService, undoManager)
                        .fetchAndMerge(entry, field),
                () -> dialogService.notify(Localization.lang("No library selected"))
        );
    }

    @Override
    public void openExternalLink() {
        identifier.get().map(DOI::asString)
                  .ifPresent(s -> NativeDesktop.openCustomDoi(s, preferences, dialogService));
    }
}
