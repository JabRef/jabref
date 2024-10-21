
package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.MergeEntriesHelper;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergingIdBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingIdBasedFetcher.class);
    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final DialogService dialogService;

    public MergingIdBasedFetcher(GuiPreferences preferences, UndoManager undoManager, BibDatabaseContext bibDatabaseContext, DialogService dialogService) {
        this.preferences = preferences;
        this.undoManager = undoManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.dialogService = dialogService;
    }

    public BackgroundTask<List<String>> fetchAndMergeBatch(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return BackgroundTask.wrap(() -> List.of());
        }

        BackgroundTask<List<String>> backgroundTask = new BackgroundTask<>() {
            @Override
            public List<String> call() {
                List<String> updatedEntries = new ArrayList<>();
                int totalEntries = entries.size();
                for (int i = 0; i < totalEntries; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    BibEntry entry = entries.get(i);
                    updateMessage(Localization.lang("Fetching entry %d of %d", i + 1, totalEntries));
                    updateProgress(i, totalEntries);
                    if (fetchAndMergeEntry(entry)) {
                        entry.getCitationKey().ifPresent(citationKey -> {
                            updatedEntries.add(citationKey);
                            LOGGER.info("Updated entry: {}", citationKey);
                        });
                    } else {
                        LOGGER.info("Entry not updated: {}", entry.getCitationKey().orElse("No citation key"));
                    }
                }
                return updatedEntries;
            }
        };

        backgroundTask.setTitle(Localization.lang("Fetching and merging entries"));
        backgroundTask.showToUser(true);

        backgroundTask.onSuccess(updatedEntries -> {
            LOGGER.info("Batch update completed. {} entries updated: {}", updatedEntries.size(), String.join(", ", updatedEntries));
            // Show notification without the citation keys
            String message = Localization.lang("%0 entries updated", updatedEntries.size());
            dialogService.notify(message);
        });

        backgroundTask.onFailure(exception -> {
            LOGGER.error("Error fetching and merging entries", exception);
            dialogService.notify(Localization.lang("Error fetching and merging entries"));
        });

        return backgroundTask;
    }

    private boolean fetchAndMergeEntry(BibEntry entry) {
        for (Field field : List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT)) {
            Optional<String> identifier = entry.getField(field);
            if (identifier.isPresent()) {
                Optional<IdBasedFetcher> fetcherOpt = getFetcherForField(field);
                if (fetcherOpt.isPresent()) {
                    try {
                        Optional<BibEntry> fetchedEntry = fetcherOpt.get().performSearchById(identifier.get());
                        if (fetchedEntry.isPresent()) {
                            return processFetchedEntry(fetchedEntry.get(), entry);
                        }
                    } catch (Exception exception) {
                        LOGGER.error("Error fetching entry with {} {}", field, identifier.get(), exception);
                    }
                }
            }
        }
        return false;
    }

    private Optional<IdBasedFetcher> getFetcherForField(Field field) {
        return switch (field) {
            case StandardField.DOI -> Optional.of(new DoiFetcher(preferences.getImportFormatPreferences()));
            case StandardField.ISBN -> Optional.of(new IsbnFetcher(preferences.getImportFormatPreferences()));
            case StandardField.EPRINT -> Optional.of(new IacrEprintFetcher(preferences.getImportFormatPreferences()));
            default -> Optional.empty();
        };
    }

    private boolean processFetchedEntry(BibEntry fetchedEntry, BibEntry originalEntry) {
        ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences()).doPostCleanup(fetchedEntry);

        NamedCompound ce = new NamedCompound(Localization.lang("Merge entry"));
        MergeEntriesHelper.mergeEntries(originalEntry, fetchedEntry, ce);

        if (ce.hasEdits()) {
            ce.end();
            undoManager.addEdit(ce);
            return true;
        }
        return false;
    }
}
