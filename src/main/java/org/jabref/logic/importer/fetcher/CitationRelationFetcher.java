package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

public class CitationRelationFetcher implements EntryBasedFetcher {

    private String title;
    private String description;

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
