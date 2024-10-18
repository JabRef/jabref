package org.jabref.gui.externalfiles;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to download fulltext PDF for selected entry(ies) by following URL or DOI link.
 */
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

        Task<Map<BibEntry, Optional<URL>>> findFullTextsTask = new Task<>() {
            @Override
            protected Map<BibEntry, Optional<URL>> call() {
                Map<BibEntry, Optional<URL>> downloads = new ConcurrentHashMap<>();
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

    private void downloadFullTexts(Map<BibEntry, Optional<URL>> downloads, BibDatabaseContext databaseContext) {
        for (Map.Entry<BibEntry, Optional<URL>> download : downloads.entrySet()) {
            BibEntry entry = download.getKey();
            Optional<URL> result = download.getValue();
            if (result.isPresent()) {
                addLinkedFileFromURL(databaseContext, result.get(), entry);
            } else {
                dialogService.notify(Localization.lang("No full text document found for entry %0.",
                        entry.getCitationKey().orElse(Localization.lang("undefined"))));
            }
        }
    }

    /**
     * This method attaches a linked file from a URL (if not already linked) to an entry using the key and value pair
     * from the findFullTexts map and then downloads the file into the given targetDirectory
     *
     * @param databaseContext the active database
     * @param url             the url "key"
     * @param entry           the entry "value"
     */
    private void addLinkedFileFromURL(BibDatabaseContext databaseContext, URL url, BibEntry entry) {
        LinkedFile newLinkedFile = new LinkedFile(url, "");

        if (!entry.getFiles().contains(newLinkedFile)) {
            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                    newLinkedFile,
                    entry,
                    databaseContext,
                    taskExecutor,
                    dialogService,
                    preferences);
            onlineFile.download(true);
        } else {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
        }
    }
}
