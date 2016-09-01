package net.sf.jabref.gui.mergeentries;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.DoiFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingworker.SwingWorker;


public class FetchAndMergeWorker extends SwingWorker<Optional<BibEntry>, Void> {

    private static final Log LOGGER = LogFactory.getLog(FetchAndMergeWorker.class);

    private final BasePanel panel;
    private final BibEntry entry;
    private final String field;
    private final Optional<String> fieldContent;
    private final String type;


    public FetchAndMergeWorker(BasePanel panel, BibEntry entry, String field) {
        this.panel = Objects.requireNonNull(panel);
        this.entry = Objects.requireNonNull(entry);
        this.field = Objects.requireNonNull(field);

        this.fieldContent = entry.getField(field);
        this.type = FieldName.getDisplayName(field);
    }

    @Override
    protected Optional<BibEntry> doInBackground() throws Exception {
        IdBasedFetcher fetcher;
        if (FieldName.DOI.equals(field)) {
            fetcher = new DoiFetcher(Globals.prefs.getImportFormatPreferences());
        } else if (FieldName.ISBN.equals(field)) {
            fetcher = new IsbnFetcher(Globals.prefs.getImportFormatPreferences());
        } else if (FieldName.EPRINT.equals(field)) {
            fetcher = new ArXiv();
        } else {
            // Should never occur
            return Optional.empty();
        }

        try {
            return fetcher.performSearchById(fieldContent.get());
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
                MergeFetchedEntryDialog dialog = new MergeFetchedEntryDialog(panel, entry, fetchedEntry.get(), type);
                dialog.setVisible(true);
            } else {
                panel.frame().setStatus(Localization.lang("Cannot get info based on given %0: %1", type, fieldContent.get()));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while fetching Entry", e);
        }
    }

}
