package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.UncheckedException;

public class CompositeIdFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        try {
            return Identifier.from(identifier)
                             .flatMap(id -> WebFetchers.getIdBasedFetcherForIdentifier(id, importFormatPreferences))
                             .flatMap(Unchecked.function(fetcher -> fetcher.performSearchById(identifier)));
        } catch (UncheckedException e) {
            throw (FetcherException) e.getCause();
        }
    }

    public String getName() {
        return "CompositeIdFetcher";
    }

    public static boolean containsValidId(String identifier) {
        return Identifier.from(identifier).isPresent();
    }
}
