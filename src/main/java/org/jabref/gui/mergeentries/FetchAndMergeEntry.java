package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for fetching and merging bibliographic information
 */
public class FetchAndMergeEntry {

    // A list of all field which are supported
    public static List<String> SUPPORTED_FIELDS = Arrays.asList(FieldName.DOI, FieldName.EPRINT, FieldName.ISBN);
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchAndMergeEntry.class);
    private final BasePanel panel;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public FetchAndMergeEntry(BasePanel panel, TaskExecutor taskExecutor) {
        this.dialogService = panel.frame().getDialogService();
        this.panel = panel;
        this.taskExecutor = taskExecutor;
    }

    public void fetchAndMerge(BibEntry entry) {
        fetchAndMerge(entry, SUPPORTED_FIELDS);
    }

    public void fetchAndMerge(BibEntry entry, String field) {
        fetchAndMerge(entry, Collections.singletonList(field));
    }

    public void fetchAndMerge(BibEntry entry, List<String> fields) {
        for (String field : fields) {
            Optional<String> fieldContent = entry.getField(field);
            if (fieldContent.isPresent()) {
                Optional<IdBasedFetcher> fetcher = WebFetchers.getIdBasedFetcherForField(field, Globals.prefs.getImportFormatPreferences());
                if (fetcher.isPresent()) {
                    BackgroundTask.wrap(() -> fetcher.get().performSearchById(fieldContent.get()))
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
                }
            } else {
                dialogService.notify(Localization.lang("No %0 found", FieldName.getDisplayName(field)));
            }
        }
    }

    public void fetchAndMerge(BibEntry entry, EntryBasedFetcher fetcher) {
        BackgroundTask.wrap(() -> fetcher.performSearch(entry).stream().findFirst())
                      .onSuccess(fetchedEntry -> {
                          if (fetchedEntry.isPresent()) {
                              MergeFetchedEntryDialog dialog = new MergeFetchedEntryDialog(panel, entry, fetchedEntry.get(), fetcher.getName());
                              dialog.setVisible(true);
                          } else {
                              dialogService.notify(Localization.lang("Could not find any bibliographic information."));
                          }
                      })
                      .onFailure(exception -> {
                          LOGGER.error("Error while fetching entry with " + fetcher.getName(), exception);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error while fetching from %0", fetcher.getName()), exception);
                      })
                      .executeWith(taskExecutor);
    }
}
