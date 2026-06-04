package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.UrlBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;

public class GenericUrlBasedFetcher implements UrlBasedFetcher {
    @Override
    public @NonNull String getName() {
        return "Generic URL";
    }

    public Optional<List<BibEntry>> performSearch(@NonNull String url) throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        entry.setField(StandardField.URL, url);
        return Optional.of(List.of(entry));
    }
}
