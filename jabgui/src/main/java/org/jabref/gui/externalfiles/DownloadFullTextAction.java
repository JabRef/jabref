package org.jabref.gui.externalfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FetcherResult;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Try to download fulltext PDF for selected entry(s) by following URL or DOI link.
///
/// [impl->req~fetchers.fulltext-background-search~1]
public class DownloadFullTextAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFullTextAction.class);
    // The minimum number of selected entries to ask the user for confirmation
    private static final int WARNING_LIMIT = 5;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final UiTaskExecutor taskExecutor;
    private final Function<BibEntry, Optional<FetcherResult>> fullTextFinder;

    public DownloadFullTextAction(DialogService dialogService,
                                  StateManager stateManager,
                                  GuiPreferences preferences,
                                  UiTaskExecutor taskExecutor) {
        this(dialogService,
                stateManager,
                preferences,
                taskExecutor,
                entry -> new FulltextFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()).findFullTextPDF(entry));
    }

    DownloadFullTextAction(DialogService dialogService,
                           StateManager stateManager,
                           GuiPreferences preferences,
                           UiTaskExecutor taskExecutor,
                           Function<BibEntry, Optional<FetcherResult>> fullTextFinder) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.fullTextFinder = fullTextFinder;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(this::execute);
    }

    private void execute(BibDatabaseContext databaseContext) {
        List<BibEntry> entries = List.copyOf(stateManager.getSelectedEntries());
        if (entries.isEmpty()) {
            LOGGER.debug("No entry selected for fulltext download.");
            return;
        }

        if (entries.size() >= WARNING_LIMIT) {
            boolean confirmDownload = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Download full text documents"),
                    Localization.lang(
                            "You are attempting to download full text documents for %0 entries.\nJabRef will send at least one request per entry to a publisher.",
                            String.valueOf(entries.size())),
                    // [impl->req~ui.dialogs.confirmation.naming~1]
                    Localization.lang("Download full text documents"),
                    Localization.lang("Cancel"));

            if (!confirmDownload) {
                dialogService.notify(Localization.lang("Operation canceled."));
                return;
            }
        }

        BackgroundTask<List<EntryDownload>> findFullTextsTask = new BackgroundTask<>() {
            @Override
            public List<EntryDownload> call() {
                List<EntryDownload> downloads = new ArrayList<>(entries.size());
                int count = 0;
                for (BibEntry entry : entries) {
                    if (isCancelled()) {
                        break;
                    }

                    BibEntry lookupSnapshot = new BibEntry(entry);
                    downloads.add(new EntryDownload(entry, lookupSnapshot, fullTextFinder.apply(lookupSnapshot)));
                    updateProgress(++count, entries.size());
                    updateMessage(Localization.lang("%0/%1 entries", count, entries.size()));
                }
                return downloads;
            }
        };

        findFullTextsTask.setTitle(Localization.lang("Download full text documents"))
                         .withInitialMessage(Localization.lang("Looking for full text document..."))
                         .showToUser(true)
                         .onSuccess(downloads -> downloadFullTexts(downloads, databaseContext))
                         .executeWith(taskExecutor);
    }

    private void downloadFullTexts(List<EntryDownload> downloads, BibDatabaseContext databaseContext) {
        if (!stateManager.getOpenDatabases().contains(databaseContext)) {
            LOGGER.debug("Library closed before the full text search finished; skipping downloads.");
            return;
        }

        for (EntryDownload download : downloads) {
            BibEntry entry = download.entry();
            if (!databaseContext.getDatabase().getEntries().contains(entry)) {
                continue;
            }
            if (!entry.equals(download.lookupSnapshot())) {
                LOGGER.debug("Entry changed during full text search; skipping download.");
                continue;
            }

            download.result().ifPresentOrElse(
                    result -> addLinkedFileFromURL(databaseContext, result, entry),
                    () -> dialogService.notify(Localization.lang("No full text document found for entry %0.",
                            entry.getCitationKey().orElse(Localization.lang("undefined")))));
        }
    }

    /// This method attaches a linked file from a URL (if not already linked) to an entry using the key and value pair
    /// from the findFullTexts map and then downloads the file into the given targetDirectory
    ///
    /// @param databaseContext the active database
    /// @param result          the fetcher result containing the URL and any required download headers
    /// @param entry           the entry "value"
    void addLinkedFileFromURL(BibDatabaseContext databaseContext, FetcherResult result, BibEntry entry) {
        LinkedFile newLinkedFile = new LinkedFile(result.source(), "");

        if (!entry.getFiles().contains(newLinkedFile)) {
            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                    newLinkedFile,
                    entry,
                    databaseContext,
                    taskExecutor,
                    dialogService,
                    preferences);
            // The URL is not linked on the entry, so there is no link to keep: a downloaded HTML page (paywall or
            // consent interstitial instead of the PDF) has to be deleted rather than left orphaned in the file directory.
            onlineFile.download(false, result.headers());
        } else {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
        }
    }

    @NullMarked
    private record EntryDownload(BibEntry entry, BibEntry lookupSnapshot, Optional<FetcherResult> result) {
    }
}
