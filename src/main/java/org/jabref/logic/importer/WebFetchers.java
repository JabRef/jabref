package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

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

    @SuppressWarnings("unchecked")
    public static <T> IdFetcher<T> getIdFetcherForIdentifier(Class<T> clazz) {
        if (clazz == DOI.class) {
            return (IdFetcher<T>) new CrossRef();
        } else {
            throw new IllegalArgumentException("No fetcher found for identifier" + clazz.getCanonicalName());
        }
    }
}
