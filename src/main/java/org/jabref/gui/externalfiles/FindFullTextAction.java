package org.jabref.gui.externalfiles;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
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

    public FindFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
        this.dialogService = basePanel.frame().getDialogService();
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(this::findFullTexts)
                      .onSuccess(this::downloadFullTexts)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private Map<Optional<URL>, BibEntry> findFullTexts() {
        if (!basePanel.getSelectedEntries().isEmpty()) {
            basePanel.output(Localization.lang("Looking for full text document..."));
        } else {
            LOGGER.debug("No entry selected for fulltext download.");
        }

        if (basePanel.getSelectedEntries().size() >= WARNING_LIMIT) {
            boolean confirmDownload = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Look up full text documents"),
                    Localization.lang(
                            "You are about to look up full text documents for %0 entries.",
                            String.valueOf(basePanel.getSelectedEntries().size())) + "\n"
                            + Localization.lang("JabRef will send at least one request per entry to a publisher.")
                            + "\n"
                            + Localization.lang("Do you still want to continue?"),
                    Localization.lang("Look up full text documents"),
                    Localization.lang("Cancel"));

            if (!confirmDownload) {
                basePanel.output(Localization.lang("Operation canceled."));
                return null;
            }
        }

        Map<Optional<URL>, BibEntry> downloads = new ConcurrentHashMap<>();
        for (BibEntry entry : basePanel.getSelectedEntries()) {
            FulltextFetchers fetchers = new FulltextFetchers(Globals.prefs.getImportFormatPreferences());
            downloads.put(fetchers.findFullTextPDF(entry), entry);
        }

        return downloads;
    }

    private void downloadFullTexts(Map<Optional<URL>, BibEntry> downloads) {
        List<Optional<URL>> finishedTasks = new ArrayList<>();
        for (Map.Entry<Optional<URL>, BibEntry> download : downloads.entrySet()) {
            BibEntry entry = download.getValue();
            Optional<URL> result = download.getKey();
            if (result.isPresent()) {
                Optional<Path> dir = basePanel.getBibDatabaseContext().getFirstExistingFileDir(Globals.prefs.getFilePreferences());

                if (!dir.isPresent()) {

                    dialogService.showErrorDialogAndWait(Localization.lang("Directory not found"),
                            Localization.lang("Main file directory not set!") + " " + Localization.lang("Preferences")
                                    + " -> " + Localization.lang("File"));

                    return;
                }

                //Download full text
                addLinkedFileFromURL(result.get(), entry);
            } else {
                dialogService.notify(Localization.lang("No full text document found for entry %0.",
                        entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            }
            finishedTasks.add(result);
        }
        for (Optional<URL> result : finishedTasks) {
            downloads.remove(result);
        }
    }

    /**
     * This method attaches a linked file from a URL (if not already linked) to an entry using the key and value pair
     * from the findFullTexts map
     *
     * @param url   the url "key"
     * @param entry the entry "value"
     */
    private void addLinkedFileFromURL(URL url, BibEntry entry) {

        LinkedFile newLinkedFile = new LinkedFile(url, "");

        if (!entry.getFiles().contains(newLinkedFile)) {

            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                    newLinkedFile,
                    entry,
                    basePanel.getBibDatabaseContext(),
                    Globals.TASK_EXECUTOR,
                    dialogService,
                    JabRefPreferences.getInstance());

            onlineFile.download();

            entry.addFile(onlineFile.getFile());

            dialogService.notify(Localization.lang("Finished downloading full text document for entry %0.",
                    entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
        } else {
            dialogService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
        }
    }
}
