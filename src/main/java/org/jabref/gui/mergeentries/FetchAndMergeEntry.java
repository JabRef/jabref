package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for fetching and merging information based on a specific field
 *
 */
public class FetchAndMergeEntry {

    // A list of all field which are supported
    public static List<String> SUPPORTED_FIELDS = Arrays.asList(FieldName.DOI, FieldName.EPRINT, FieldName.ISBN);
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchAndMergeEntry.class);

    private final BasePanel panel;
    private DialogService dialogService;
    
    /**
     * Convenience constructor for a single field
     *
     * @param entry - BibEntry to fetch information for
     * @param panel - current BasePanel
     * @param field - field to get information from
     */
    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, String field) {
        this(entry, panel, Arrays.asList(field));
    }

    public FetchAndMergeEntry(BibEntry entry, String field) {
        this(entry, JabRefGUI.getMainFrame().getCurrentBasePanel(), field);
    }

    /**
     * Default constructor
     *
     * @param entry - BibEntry to fetch information for
     * @param panel - current BasePanel
     * @param fields - List of fields to get information from, one at a time in given order
     */
    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, List<String> fields) {
        this.dialogService = panel.frame().getDialogService();
        this.panel = panel;

        // TODO: Don't run this method as part of the constructor
        fetchAndMerge(entry, fields);
    }

    public void fetchAndMerge(BibEntry entry, List<String> fields) {
        for (String field : fields) {
            Optional<String> fieldContent = entry.getField(field);
            if (fieldContent.isPresent()) {
                BackgroundTask.wrap(() -> {
                    Optional<IdBasedFetcher> fetcher = WebFetchers.getIdBasedFetcherForField(field, Globals.prefs.getImportFormatPreferences());
                    if (fetcher.isPresent()) {
                        return fetcher.get().performSearchById(fieldContent.get());
                    } else {
                        return Optional.<BibEntry>empty();
                    }
                })
                              .onSuccess(fetchedEntry -> {
                                  String type = FieldName.getDisplayName(field);
                                  if (fetchedEntry.isPresent()) {
                                      MergeFetchedEntryDialog dialog = new MergeFetchedEntryDialog(panel, entry, fetchedEntry.get(), type);
                                      dialog.setVisible(true);
                                  } else {
                                      panel.frame().setStatus(Localization.lang("Cannot get info based on given %0: %1", type, fieldContent.get()));
                                  }
                              })
                              .onFailure(exception -> {
                                  LOGGER.error("Error while fetching bibliographic information", exception);
                                  dialogService.showErrorDialogAndWait(exception);
                              })
                              .executeWith(Globals.TASK_EXECUTOR);
            } else {
                dialogService.notify(Localization.lang("No %0 found", FieldName.getDisplayName(field)));
            }
        }
    }
}
