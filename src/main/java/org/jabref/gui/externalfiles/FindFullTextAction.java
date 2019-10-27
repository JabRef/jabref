package org.jabref.gui.externalfiles;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
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
import org.jabref.gui.undo.NamedCompound;
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
public class FindFullTextAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(FindFullTextAction.class);
    // The minimum number of selected entries to ask the user for confirmation
    private static final int WARNING_LIMIT = 5;

    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final FulltextFetchers fetchers = new FulltextFetchers(Globals.prefs.getImportFormatPreferences());

    public FindFullTextAction(BasePanel basePanel) {
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
                    Localization.lang("Look up full text documents"),
                    Localization.lang("You are about to look up full text documents for %0 entries.",
                                    String.valueOf(basePanel.getSelectedEntries().size())) + "\n"
                                                                    + Localization.lang("JabRef will send at least one request per entry to a publisher.")
                                                                    + "\n"
                                                                    + Localization.lang("Do you still want to continue?"),
                    Localization.lang("Look up full text documents"),
                    Localization.lang("Cancel"));

            if (!confirmDownload) {
                basePanel.output(Localization.lang("Operation canceled."));
                return;
            }
        }

        lookupFullText();
    }

    private void lookupFullText() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(basePanel.getBibDatabaseContext(), Globals.prefs.getFilePreferences(), Globals.prefs.getAutoLinkPreferences(), ExternalFileTypes.getInstance());

        Task<List<BibEntry>> linkFilesTask = new Task<List<BibEntry>>() {

            @Override
            protected List<BibEntry> call() {
                return util.linkAssociatedFiles(basePanel.getSelectedEntries(), new NamedCompound(""));
            }

            @Override
            protected void succeeded() {
                Map<BibEntry, Optional<URL>> downloads = new ConcurrentHashMap<>();

                for (BibEntry entry : getValue()) {
                    if (entry.getFiles().isEmpty()) {
                        downloads.put(entry, fetchers.findFullTextPDF(entry));
                    } else {
                        dialogService.notify(Localization.lang("Full text document found already on disk for entry %0. Not downloaded again.",
                                                               entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
                    }
                }
                downloadMissingFullTexts(downloads);
            }
        };

        dialogService.showProgressDialogAndWait(Localization.lang("Look up full text documents"),
                                                Localization.lang("Looking for full text document..."),
                                                linkFilesTask);
        Globals.TASK_EXECUTOR.execute(linkFilesTask);

    }

    private void downloadMissingFullTexts(Map<BibEntry, Optional<URL>> downloads) {
        for (Map.Entry<BibEntry, Optional<URL>> download : downloads.entrySet()) {
            BibEntry entry = download.getKey();
            Optional<URL> result = download.getValue();
            if (result.isPresent()) {
                Optional<Path> dir = basePanel.getBibDatabaseContext().getFirstExistingFileDir(Globals.prefs.getFilePreferences());

                if (!dir.isPresent()) {

                    dialogService.showErrorDialogAndWait(Localization.lang("Directory not found"),
                                                         Localization.lang("Main file directory not set!") + " "
                                                           + Localization.lang("Preferences")
                                                           + " -> " + Localization.lang("File"));
                    return;
                }
                //Download and link full text
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
