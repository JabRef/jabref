package org.jabref.gui.externalfiles;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.importer.FetcherResult;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Try to download fulltext PDF for selected entry(s) by following URL or DOI link.
public class DownloadFullTextAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFullTextAction.class);
    // The minimum number of selected entries to ask the user for confirmation
    private static final int WARNING_LIMIT = 5;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final UiTaskExecutor taskExecutor;

    public DownloadFullTextAction(DialogService dialogService,
                                  StateManager stateManager,
                                  GuiPreferences preferences,
                                  UiTaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        List<BibEntry> entries = stateManager.getSelectedEntries();
        if (entries.isEmpty()) {
            LOGGER.debug("No entry selected for fulltext download.");
            return;
        }

        dialogService.notify(Localization.lang("Looking for full text document..."));

        if (entries.size() >= WARNING_LIMIT) {
            boolean confirmDownload = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Download full text documents"),
                    Localization.lang(
                            "You are attempting to download full text documents for %0 entries.\nJabRef will send at least one request per entry to a publisher.",
                            String.valueOf(stateManager.getSelectedEntries().size())),
                    // [impl->req~ui.dialogs.confirmation.naming~1]
                    Localization.lang("Download full text documents"),
                    Localization.lang("Cancel"));

            if (!confirmDownload) {
                dialogService.notify(Localization.lang("Operation canceled."));
                return;
            }
        }

        Task<Map<BibEntry, Optional<FetcherResult>>> findFullTextsTask = new Task<>() {
            @Override
            protected Map<BibEntry, Optional<FetcherResult>> call() {
                Map<BibEntry, Optional<FetcherResult>> downloads = new ConcurrentHashMap<>();
                int count = 0;
                for (BibEntry entry : entries) {
                    FulltextFetchers fetchers = new FulltextFetchers(
                            preferences.getImportFormatPreferences(),
                            preferences.getImporterPreferences());
                    downloads.put(entry, fetchers.findFullTextPDF(entry));
                    updateProgress(++count, entries.size());
                }
                return downloads;
            }
        };

        findFullTextsTask.setOnSucceeded(value ->
                downloadFullTexts(findFullTextsTask.getValue(), stateManager.getActiveDatabase().get()));

        dialogService.showProgressDialog(
                Localization.lang("Download full text documents"),
                Localization.lang("Looking for full text document..."),
                findFullTextsTask);

        taskExecutor.execute(findFullTextsTask);
    }

    private void downloadFullTexts(Map<BibEntry, Optional<FetcherResult>> downloads, BibDatabaseContext databaseContext) {
        for (Map.Entry<BibEntry, Optional<FetcherResult>> download : downloads.entrySet()) {
            BibEntry entry = download.getKey();
            Optional<FetcherResult> result = download.getValue();
            if (result.isPresent()) {
                addLinkedFileFromURL(databaseContext, result.get(), entry);
            } else {
                dialogService.notify(Localization.lang("No full text document found for entry %0.",
                        entry.getCitationKey().orElse(Localization.lang("undefined"))));
            }
        }
    }

    /// This method attaches a linked file from a URL (if not already linked) to an entry using the key and value pair
    /// from the findFullTexts map and then downloads the file into the given targetDirectory
    ///
    /// @param databaseContext the active database
    /// @param result          the fetcher result containing the URL and any required download headers
    /// @param entry           the entry "value"
    private void addLinkedFileFromURL(BibDatabaseContext databaseContext, FetcherResult result, BibEntry entry) {
        URL source = result.source();
        // Browser-extension / local-cache fetchers return a file:// URL pointing
        // at a PDF already on disk. Route to a local-attach path so the same
        // move-and-rename pipeline a successful HTTP download would use kicks in;
        // online URLs keep the existing LinkedFileViewModel.download path.
        if ("file".equalsIgnoreCase(source.getProtocol())) {
            attachLocalFile(databaseContext, source, entry);
            return;
        }

        LinkedFile newLinkedFile = new LinkedFile(source, "");

        if (!entry.getFiles().contains(newLinkedFile)) {
            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                    newLinkedFile,
                    entry,
                    databaseContext,
                    taskExecutor,
                    dialogService,
                    preferences);
            onlineFile.download(true, result.headers());
        } else {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
        }
    }

    private void attachLocalFile(BibDatabaseContext databaseContext, URL url, BibEntry entry) {
        Path sourcePath;
        try {
            sourcePath = Path.of(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            LOGGER.warn("Could not interpret fetcher-returned file URL {}", url, e);
            dialogService.notify(Localization.lang("No full text document found for entry %0.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
            return;
        }

        ExternalFileTypes.getExternalFileTypeByExt("pdf", preferences.getExternalApplicationsPreferences())
                         .ifPresent(externalFileType ->
                                 attachLocalPdf(databaseContext, sourcePath, externalFileType, entry));
    }

    private void attachLocalPdf(BibDatabaseContext databaseContext, Path sourcePath,
                                ExternalFileType externalFileType, BibEntry entry) {
        LinkedFile linkedFile = new LinkedFile("", sourcePath, externalFileType.getName());

        if (entry.getFiles().contains(linkedFile)) {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
            return;
        }

        LinkedFileHandler handler = new LinkedFileHandler(
                linkedFile, entry, databaseContext, preferences.getFilePreferences());

        // copyOrMoveToDefaultDirectory does blocking filesystem I/O; run it
        // off the JavaFX Application Thread. shouldMove=true,
        // shouldRenameToFilenamePattern=true — same fate a successful HTTP
        // download would have via DownloadLinkedFileAction.
        BackgroundTask
                .wrap(() -> {
                    handler.copyOrMoveToDefaultDirectory(true, true);
                    return linkedFile;
                })
                .onSuccess(entry::addFile)
                .onFailure(e -> {
                    LOGGER.warn("Could not move fetcher-returned file {} into the library directory",
                            sourcePath, e);
                    entry.addFile(linkedFile);
                })
                .executeWith(taskExecutor);
    }
}
