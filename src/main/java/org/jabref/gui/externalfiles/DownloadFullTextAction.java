package org.jabref.gui.externalfiles;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.concurrent.Task;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to download fulltext PDF for selected entry(ies) by following URL or DOI link.
 */
public class DownloadFullTextAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFullTextAction.class);
    // The minimum number of selected entries to ask the user for confirmation
    private static final int WARNING_LIMIT = 5;

    private final BasePanel basePanel;
    private final DialogService dialogService;

    public DownloadFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
        this.dialogService = basePanel.frame().getDialogService();
    }

    @Override
    public void execute() {
        if (!basePanel.getSelectedEntries().isEmpty()) {
            basePanel.output(Localization.lang("Looking for full text document..."));
        } else {
            LOGGER.debug("No entry selected for fulltext download.");
        }

        if (basePanel.getSelectedEntries().size() >= WARNING_LIMIT) {
            boolean confirmDownload = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Download full text documents"),
                    Localization.lang(
                            "You are about to download full text documents for %0 entries.",
                            String.valueOf(basePanel.getSelectedEntries().size())) + "\n"
                            + Localization.lang("JabRef will send at least one request per entry to a publisher.")
                            + "\n"
                            + Localization.lang("Do you still want to continue?"),
                    Localization.lang("Download full text documents"),
                    Localization.lang("Cancel"));

            if (!confirmDownload) {
                basePanel.output(Localization.lang("Operation canceled."));
                return;
            }
        }

        Task<Map<BibEntry, Optional<URL>>> findFullTextsTask = new Task<Map<BibEntry, Optional<URL>>>() {
            @Override
            protected Map<BibEntry, Optional<URL>> call() {
                Map<BibEntry, Optional<URL>> downloads = new ConcurrentHashMap<>();
                int count = 0;
                for (BibEntry entry : basePanel.getSelectedEntries()) {
                    FulltextFetchers fetchers = new FulltextFetchers(Globals.prefs.getImportFormatPreferences());
                    downloads.put(entry, fetchers.findFullTextPDF(entry));
                    updateProgress(++count, basePanel.getSelectedEntries().size());
                }
                return downloads;
            }
        };

        findFullTextsTask.setOnSucceeded(value -> downloadFullTexts(findFullTextsTask.getValue()));

        dialogService.showProgressDialogAndWait(
                Localization.lang("Download full text documents"),
                Localization.lang("Looking for full text document..."),
                findFullTextsTask);

        Globals.TASK_EXECUTOR.execute(findFullTextsTask);
    }

    private void downloadFullTexts(Map<BibEntry, Optional<URL>> downloads) {
        for (Map.Entry<BibEntry, Optional<URL>> download : downloads.entrySet()) {
            BibEntry entry = download.getKey();
            Optional<URL> result = download.getValue();
            if (result.isPresent()) {
                Optional<Path> dir = basePanel.getBibDatabaseContext().getFirstExistingFileDir(Globals.prefs.getFilePreferences());
                if (dir.isEmpty()) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Directory not found"),
                            Localization.lang("Main file directory not set!") + " " + Localization.lang("Preferences")
                                    + " -> " + Localization.lang("File"));
                    return;
                }

                // Download and link full text
                addLinkedFileFromURL(result.get(), entry, dir.get());
            } else {
                dialogService.notify(Localization.lang("No full text document found for entry %0.",
                        entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            }
        }
    }

    /**
     * This method attaches a linked file from a URL (if not already linked) to an entry using the key and value pair
     * from the findFullTexts map and then downloads the file into the given targetDirectory
     *
     * @param url   the url "key"
     * @param entry the entry "value"
     * @param targetDirectory the target directory for the downloaded file
     */
    private void addLinkedFileFromURL(URL url, BibEntry entry, Path targetDirectory) {
        LinkedFile newLinkedFile = new LinkedFile(url, "");

        if (!entry.getFiles().contains(newLinkedFile)) {
            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                    newLinkedFile,
                    entry,
                    basePanel.getBibDatabaseContext(),
                    Globals.TASK_EXECUTOR,
                    dialogService,
                    JabRefPreferences.getInstance().getXMPPreferences(),
                    JabRefPreferences.getInstance().getFilePreferences(),
                    ExternalFileTypes.getInstance());

            try {
                URLDownload urlDownload = new URLDownload(newLinkedFile.getLink());
                BackgroundTask<Path> downloadTask = onlineFile.prepareDownloadTask(targetDirectory, urlDownload);
                downloadTask.onSuccess(destination -> {
                    LinkedFile downloadedFile = LinkedFilesEditorViewModel.fromFile(
                            destination,
                            basePanel.getBibDatabaseContext().getFileDirectoriesAsPaths(JabRefPreferences.getInstance().getFilePreferences()), ExternalFileTypes.getInstance());
                    entry.addFile(downloadedFile);
                    dialogService.notify(Localization.lang("Finished downloading full text document for entry %0.",
                            entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
                });
                Globals.TASK_EXECUTOR.execute(downloadTask);
            } catch (MalformedURLException exception) {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid URL"), exception);
            }
        } else {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
        }
    }
}
