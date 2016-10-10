package net.sf.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper around {@link SearchBasedFetcher} which implements the old {@link EntryFetcher} interface.
 */
public class SearchBasedEntryFetcher implements EntryFetcher{

    private static final Log LOGGER = LogFactory.getLog(SearchBasedEntryFetcher.class);
    private final SearchBasedFetcher fetcher;

    public SearchBasedEntryFetcher(SearchBasedFetcher fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher);
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {

        status.setStatus(Localization.lang("Processing %0", query));
        try {
            List<BibEntry> matches = fetcher.performSearch(query);
            matches.forEach(inspector::addEntry);
            return !matches.isEmpty();
        } catch (FetcherException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)inspector).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    public String getTitle() {
        return fetcher.getName();
    }

    @Override
    public HelpFile getHelpPage() {
        return fetcher.getHelpPage();
    }

    @Override
    public JPanel getOptionsPanel() {
        // not supported
        return null;
    }

    @Override
    public void stopFetching() {
        // not supported
    }
}
