package net.sf.jabref.gui.mergeentries;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.WebFetchers;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class FetchAndMergeWorker extends SwingWorker<Optional<BibEntry>, Void> {

    private static final Log LOGGER = LogFactory.getLog(FetchAndMergeWorker.class);

    private final BasePanel panel;
    private final BibEntry entry;
    private final String field;
    private final Optional<String> fieldContent;


    public FetchAndMergeWorker(BasePanel panel, BibEntry entry, String field) {
        this.panel = Objects.requireNonNull(panel);
        this.entry = Objects.requireNonNull(entry);
        this.field = Objects.requireNonNull(field);

        this.fieldContent = entry.getField(field);
    }

    @Override
    protected Optional<BibEntry> doInBackground() throws Exception {
        Optional<IdBasedFetcher> fetcher = WebFetchers.getIdBasedFetcherForField(field, Globals.prefs.getImportFormatPreferences());

        try {
            Optional<String> fieldContentValue = fieldContent;
            if (fieldContentValue.isPresent() && fetcher.isPresent()) {
                return fetcher.get().performSearchById(fieldContentValue.get());
            } else {
                return Optional.empty();
            }
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
            String type = FieldName.getDisplayName(field);
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
