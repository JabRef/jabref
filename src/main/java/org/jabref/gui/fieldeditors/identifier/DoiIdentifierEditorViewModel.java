package org.jabref.gui.fieldeditors.identifier;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<DOI> {
    public static final Logger LOGGER = LoggerFactory.getLogger(DoiIdentifierEditorViewModel.class);

    public DoiIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, DialogService dialogService, TaskExecutor taskExecutor, PreferencesService preferences) {
        super(StandardField.DOI, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences);
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
                    entry.setField(field, identifier.get().getNormalized());
                } else {
                    dialogService.notify(Localization.lang("No %0 found", field.getDisplayName()));
                }
            }).onFailure(e -> handleIdentifierFetchingError(e, doiFetcher)).executeWith(taskExecutor);
    }

    @Override
    public void fetchBibliographyInformation(BibEntry bibEntry) {
        new FetchAndMergeEntry(JabRefGUI.getMainFrame().getCurrentLibraryTab(), taskExecutor, preferences, dialogService).fetchAndMerge(entry, field);
    }

    @Override
    public void openExternalLink() {
        identifier.get().map(DOI::getDOI)
                  .ifPresent(s -> JabRefDesktop.openCustomDoi(s, preferences, dialogService));
    }
}
