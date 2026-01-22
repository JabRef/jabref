package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.SSRN;

public class SsrnFetcher extends DoiFetcher {
    public SsrnFetcher(ImportFormatPreferences preferences) {
        super(preferences);
    }

    @Override
    public String getName() {
        return "SSRN";
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String doi = SSRN.parse(identifier).map(ssrn -> ssrn.toDoi().asString()).orElse(identifier);
        return super.performSearchById(doi);
    }
}
