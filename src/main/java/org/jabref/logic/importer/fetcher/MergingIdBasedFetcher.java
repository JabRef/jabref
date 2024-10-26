
package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class fetches bibliographic information from external sources based on the identifiers
 * (e.g. DOI, ISBN, arXiv ID) of a {@link BibEntry} and merges the fetched information into the entry.
 */
public class MergingIdBasedFetcher {
    private static final List<StandardField> SUPPORTED_FIELDS =
            List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT);

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingIdBasedFetcher.class);
    private final ImportFormatPreferences importFormatPreferences;

    public MergingIdBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<FetcherResult> fetchEntry(BibEntry entry) {
        LOGGER.debug("Entry {}", entry);

        return SUPPORTED_FIELDS.stream()
                               .map(field -> tryFetch(entry, field))
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .findFirst();
    }

    private Optional<FetcherResult> tryFetch(BibEntry entry, Field field) {
        return entry.getField(field)
                    .flatMap(identifier -> getFetcherForField(field)
                            .flatMap(fetcher -> fetchEntry(fetcher, field, identifier, entry)));
    }

    private Optional<FetcherResult> fetchEntry(IdBasedFetcher fetcher, Field field,
                                               String identifier, BibEntry entryFromLibrary) {
        try {
            LOGGER.debug("Entry {}",
                    entryFromLibrary);

            return fetcher.performSearchById(identifier)
                          .map(fetchedEntry -> {
                              BibEntry mergedEntry = (BibEntry) entryFromLibrary.clone();
                              boolean hasChanges = mergeBibEntry(fetchedEntry, mergedEntry);
                              return new FetcherResult(entryFromLibrary, mergedEntry, hasChanges);
                          });
        } catch (Exception exception) {
            LOGGER.error("Error fetching entry with {} identifier: {}",
                    field, identifier, exception);
            return Optional.empty();
        }
    }

    private boolean mergeBibEntry(BibEntry source, BibEntry target) {
        boolean hasChanges = false;
        for (Field field : source.getFields()) {
            Optional<String> sourceValue = source.getField(field);
            Optional<String> targetValue = target.getField(field);

            if (sourceValue.isPresent() && targetValue.isEmpty()) {
                target.setField(field, sourceValue.get());
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    private Optional<IdBasedFetcher> getFetcherForField(Field field) {
        return switch (field) {
            case StandardField.DOI -> Optional.of(new DoiFetcher(importFormatPreferences));
            case StandardField.ISBN -> Optional.of(new IsbnFetcher(importFormatPreferences));
            case StandardField.EPRINT -> Optional.of(new IacrEprintFetcher(importFormatPreferences));
            default -> Optional.empty();
        };
    }

    public record FetcherResult(BibEntry entryFromLibrary, BibEntry mergedEntry, boolean hasChanges) {
    }
}
