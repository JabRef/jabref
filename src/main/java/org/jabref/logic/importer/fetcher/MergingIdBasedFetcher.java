
package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergingIdBasedFetcher {
    private static final List<StandardField> SUPPORTED_FIELDS =
            List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT);

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingIdBasedFetcher.class);
    private final ImportFormatPreferences importFormatPreferences;

    public MergingIdBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<FetcherResult> fetchEntry(BibEntry entry) {
        logEntryIdentifiers(entry);

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
                                               String identifier, BibEntry originalEntry) {
        try {
            LOGGER.debug("Attempting to fetch entry using {} fetcher", field);
            logEntryDetails(originalEntry, field, identifier);

            return fetcher.performSearchById(identifier)
                          .map(fetchedEntry -> {
                              BibEntry mergedEntry = (BibEntry) originalEntry.clone();
                              boolean hasChanges = mergeBibEntry(mergedEntry, fetchedEntry);
                              return new FetcherResult(originalEntry, mergedEntry, hasChanges);
                          });
        } catch (Exception exception) {
            LOGGER.error("Error fetching entry with {} identifier: {}",
                    field, identifier, exception);
            return Optional.empty();
        }
    }

    private boolean mergeBibEntry(BibEntry target, BibEntry source) {
        boolean hasChanges = false;
        for (Field field : source.getFields()) {
            Optional<String> sourceValue = source.getField(field);
            Optional<String> targetValue = target.getField(field);
            if (sourceValue.isPresent() && !sourceValue.equals(targetValue)) {
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

    private void logEntryIdentifiers(BibEntry entry) {
        List<String> availableIds = Stream.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT)
                                          .flatMap(field -> entry.getField(field)
                                                                 .map(value -> field +
                                                                         ": " +
                                                                         value)
                                                                 .stream())
                                          .collect(Collectors.toList());

        String citationKey = entry.getCitationKey()
                                  .map(key -> "citation key: " + key)
                                  .orElse("no citation key");

        LOGGER.debug("Processing entry with {} and identifiers: {}",
                citationKey,
                availableIds.isEmpty() ? "none" : String.join(", ", availableIds));
    }

    private void logEntryDetails(BibEntry entry, Field field, String identifier) {
        StringBuilder details = new StringBuilder();
        details.append(field).append(" identifier: ").append(identifier);

        entry.getCitationKey().ifPresent(key -> details.append(", citation key: ").append(key));
        entry.getField(StandardField.TITLE).ifPresent(title -> details.append(", title: ").append(title));
        entry.getField(StandardField.AUTHOR).ifPresent(author -> details.append(", author: ").append(author));

        LOGGER.debug("Entry details - {}", details);
    }

    public record FetcherResult(BibEntry originalEntry, BibEntry mergedEntry, boolean hasChanges) {
        public BibEntry getMergedEntry() {
            return mergedEntry;
        }
    }
}
