
package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches and merges bibliographic information from external sources into existing BibEntry objects.
 * Supports multiple identifier types (DOI, ISBN, Eprint) and attempts fetching in a defined order
 * until successful.
 * The merging only adds new fields from the fetched entry and does not modify existing fields
 * in the library entry.
 */
public class MergingIdBasedFetcher {

    public record FetcherResult(BibEntry entryFromLibrary, BibEntry mergedEntry, boolean hasChanges) {
    }

    private static final List<StandardField> SUPPORTED_FIELDS =
            List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT);

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingIdBasedFetcher.class);
    private final ImportFormatPreferences importFormatPreferences;

    public MergingIdBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<FetcherResult> fetchEntry(BibEntry entryFromLibrary) {
        LOGGER.debug("Processing library entry {}", entryFromLibrary);

        return SUPPORTED_FIELDS.stream()
                               .map(field -> tryFetch(entryFromLibrary, field))
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .findFirst();
    }

    private Optional<FetcherResult> tryFetch(BibEntry entryFromLibrary, Field field) {
        return entryFromLibrary.getField(field)
                               .flatMap(identifier -> getFetcherForField(field)
                                       .flatMap(fetcher -> performFetch(fetcher, field, identifier, entryFromLibrary)));
    }

    private Optional<FetcherResult> performFetch(IdBasedFetcher fetcher, Field field,
                                                 String identifier, BibEntry entryFromLibrary) {
        try {
            return attemptFetch(fetcher, identifier, entryFromLibrary);
        } catch (Exception exception) {
            LOGGER.error("Error fetching entry with {} identifier: {}", field, identifier, exception);
            return Optional.empty();
        }
    }

    private Optional<FetcherResult> attemptFetch(IdBasedFetcher fetcher, String identifier,
                                                 BibEntry entryFromLibrary) throws FetcherException {
        return fetcher.performSearchById(identifier)
                      .map(entryFromFetcher -> {
                          BibEntry mergedEntry = (BibEntry) entryFromLibrary.clone();
                          boolean hasChanges = mergeBibEntry(entryFromFetcher, mergedEntry);
                          return new FetcherResult(entryFromLibrary, mergedEntry, hasChanges);
                      });
    }

    private boolean mergeBibEntry(BibEntry entryFromFetcher, BibEntry entryFromLibrary) {
        return entryFromFetcher.getFields().stream()
                               .filter(field -> isNewFieldFromFetcher(entryFromFetcher, entryFromLibrary, field))
                               .map(field -> {
                                   entryFromFetcher.getField(field)
                                                   .ifPresent(value -> entryFromLibrary.setField(field, value));
                                   return true;
                               })
                               .findAny()
                               .orElse(false);
    }

    private boolean isNewFieldFromFetcher(BibEntry entryFromFetcher, BibEntry entryFromLibrary, Field field) {
        Optional<String> fetcherValue = entryFromFetcher.getField(field);
        Optional<String> libraryValue = entryFromLibrary.getField(field);

        return fetcherValue.isPresent() && libraryValue.isEmpty();
    }

    private Optional<IdBasedFetcher> getFetcherForField(Field field) {
        return switch (field) {
            case StandardField.DOI -> Optional.of(new DoiFetcher(importFormatPreferences));
            case StandardField.ISBN -> Optional.of(new IsbnFetcher(importFormatPreferences));
            case StandardField.EPRINT -> Optional.of(new IacrEprintFetcher(importFormatPreferences));
            default -> Optional.empty();
        };
    }
}
