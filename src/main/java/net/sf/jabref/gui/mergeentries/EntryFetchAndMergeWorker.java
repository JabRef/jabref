package net.sf.jabref.gui.mergeentries;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntryFetchAndMergeWorker extends SwingWorker<Optional<BibEntry>, Void> {

    private static final Log LOGGER = LogFactory.getLog(EntryFetchAndMergeWorker.class);

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
            LOGGER.error("Info cannot be found", e);
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
            LOGGER.error("Error while fetching Entry", e);
        }
    }

}
