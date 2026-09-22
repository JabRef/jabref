package org.jabref.gui.fieldeditors.identifier;

import java.util.IdentityHashMap;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.formatter.bibtexfields.ShortenDOIFormatter;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<DOI> {

    /// Running lookups per entry, so the progress indicator survives the entry editor rebuilding its
    /// field editors on an entry switch. Entries are compared by identity because the lookup changes
    /// the entry. Only touched on the JavaFX thread.
    private static final ObservableMap<BibEntry, Integer> RUNNING_LOOKUPS = FXCollections.observableMap(new IdentityHashMap<>());
    private static final Logger LOGGER = LoggerFactory.getLogger(DoiIdentifierEditorViewModel.class);

    private final ShortenDOIFormatter shortenDOIFormatter;

    public DoiIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                        FieldCheckers fieldCheckers,
                                        DialogService dialogService,
                                        TaskExecutor taskExecutor,
                                        GuiPreferences preferences,
                                        UndoManager undoManager,
                                        StateManager stateManager) {
        super(StandardField.DOI, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
        this.shortenDOIFormatter = new ShortenDOIFormatter();
        configure(true, true, true);
    }

    @Override
    public void lookupIdentifier(BibEntry bibEntry) {
        CrossRef doiFetcher = new CrossRef(preferences.getImporterPreferences());

        BibEntry lookedUpEntry = entry;
        BackgroundTask.wrap(() -> doiFetcher.findIdentifier(lookedUpEntry))
                      .onRunning(() -> RUNNING_LOOKUPS.merge(lookedUpEntry, 1, Integer::sum))
                      .onFinished(() -> RUNNING_LOOKUPS.computeIfPresent(lookedUpEntry, (_, count) -> count == 1 ? null : count - 1))
                      .onSuccess(identifier -> identifier.ifPresentOrElse(
                              doi -> lookedUpEntry.setField(field, doi.asString()),
                              () -> dialogService.notify(Localization.lang("No %0 found", FieldTextMapper.getDisplayName(field))))).onFailure(e -> handleIdentifierFetchingError(e, doiFetcher)).executeWith(taskExecutor);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        identifierLookupInProgress.bind(Bindings.createBooleanBinding(() -> RUNNING_LOOKUPS.containsKey(entry), RUNNING_LOOKUPS));
    }

    @Override
    public void fetchBibliographyInformation(BibEntry bibEntry) {
        stateManager.getActiveDatabase().ifPresentOrElse(
                databaseContext -> new FetchAndMergeEntry(databaseContext, taskExecutor, preferences, dialogService, undoManager, stateManager)
                        .fetchAndMerge(entry, field),
                () -> dialogService.notify(Localization.lang("No library selected"))
        );
    }

    @Override
    public void openExternalLink() {
        identifier.get().map(DOI::asString)
                  .ifPresent(s -> NativeDesktop.openCustomDoi(s, preferences, dialogService));
    }

    @Override
    public void shortenID() {
        entry.getField(field).ifPresent(doi -> {
            String shortenedDOI = shortenDOIFormatter.format(doi);
            entry.setField(field, shortenedDOI);
            if (shortenedDOI.equals(doi)) {
                LOGGER.info("DOI is already shortened");
                dialogService.notify(Localization.lang("DOI is already shortened"));
            } else {
                LOGGER.info("Shortened DOI: {} to {}", doi, shortenedDOI);
                dialogService.notify(Localization.lang("Shortened DOI to: %0", shortenedDOI));
            }
        });
    }
}
