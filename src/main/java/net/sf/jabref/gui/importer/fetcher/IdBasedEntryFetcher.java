package net.sf.jabref.gui.importer.fetcher;

import java.util.Objects;
import java.util.Optional;

import javax.swing.JPanel;

import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IdBasedEntryFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(IdBasedEntryFetcher.class);
    private final IdBasedFetcher fetcher;

    public IdBasedEntryFetcher(IdBasedFetcher fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher);
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {

        status.setStatus(Localization.lang("Processing %0", query));
        try {
            Optional<BibEntry> match = fetcher.performSearchById(query);
            match.ifPresent(inspector::addEntry);
            return match.isPresent();
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
