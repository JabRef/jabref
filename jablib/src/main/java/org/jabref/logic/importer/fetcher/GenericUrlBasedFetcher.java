package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.WebFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

public class GenericUrlBasedFetcher implements WebFetcher {

    @Override
    public String getName() {
        return "Generic URL Fetcher";
    }
    
    public List<BibEntry> fetchEntryFromUrl(String url) throws FetcherException {

        if (url == null || url.trim().isEmpty()) {
            return List.of();
        }

        String trimmedUrl = url.trim();

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.URL, trimmedUrl);

        return List.of(entry);
    }
}
