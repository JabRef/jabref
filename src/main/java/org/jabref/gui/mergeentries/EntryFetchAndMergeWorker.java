package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.jabref.Logger;
import org.jabref.gui.BasePanel;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class EntryFetchAndMergeWorker extends SwingWorker<Optional<BibEntry>, Void> {


    private final BasePanel panel;
    private final BibEntry entry;
    private final EntryBasedFetcher fetcher;

    public EntryFetchAndMergeWorker(BasePanel panel, BibEntry entry, EntryBasedFetcher fetcher) {
        this.panel = Objects.requireNonNull(panel);
        this.entry = Objects.requireNonNull(entry);
        this.fetcher = Objects.requireNonNull(fetcher);
    }

    @Override
    protected Optional<BibEntry> doInBackground() throws Exception {
        try {
            List<BibEntry> fetchedEntries = fetcher.performSearch(entry);
            return fetchedEntries.stream().findFirst();
        } catch (FetcherException e) {
            Logger.error(this, "Info cannot be found", e);
            return Optional.empty();
        }
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            return;
        }

        try {
            Optional<BibEntry> fetchedEntry = get();
            if (fetchedEntry.isPresent()) {
                MergeFetchedEntryDialog dialog = new MergeFetchedEntryDialog(panel, entry, fetchedEntry.get(),
                        fetcher.getName());
                dialog.setVisible(true);
            } else {
                panel.frame().setStatus(Localization.lang("Could not find any bibliographic information."));
            }
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(this, "Error while fetching Entry", e);
        }
    }

}
