package net.sf.jabref.logic.importer;

import java.util.Optional;

import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.DoiFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnFetcher;
import net.sf.jabref.model.entry.FieldName;

public class WebFetchers {

    public static Optional<IdBasedFetcher> getIdBasedFetcherForField(String field, ImportFormatPreferences preferences) {
        IdBasedFetcher fetcher;
        switch (field) {
            case FieldName.DOI:
                fetcher = new DoiFetcher(preferences);
                break;
            case FieldName.ISBN:
                fetcher = new IsbnFetcher(preferences);
                break;
            case FieldName.EPRINT:
                fetcher = new ArXiv(preferences);
                break;
            default:
                return Optional.empty();
        }
        return Optional.of(fetcher);
    }
}
