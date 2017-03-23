package org.jabref.gui.importer.fetcher;

import java.util.Objects;
import java.util.Optional;

import javax.swing.JPanel;

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

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
