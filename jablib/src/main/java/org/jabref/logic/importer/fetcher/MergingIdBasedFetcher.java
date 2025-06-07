package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.layout.format.NonSpaceWhitespaceRemover;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetches and merges bibliographic information from external sources into existing BibEntry objects.
/// Supports multiple identifier types (DOI, ISBN, Eprint) and attempts fetching in a defined order
/// until successful.
/// The merging only adds new fields from the fetched entry and does not modify existing fields
/// in the library entry.
public class MergingIdBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingIdBasedFetcher.class);
    private static final List<StandardField> SUPPORTED_FIELDS =
            List.of(StandardField.DOI, StandardField.ISBN, StandardField.EPRINT);
    private static final NonSpaceWhitespaceRemover REMOVE_WHITESPACE_FORMATTER = new NonSpaceWhitespaceRemover();
    private final ImportFormatPreferences importFormatPreferences;

    public MergingIdBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public record FetcherResult(
            BibEntry entryFromLibrary,
            BibEntry mergedEntry,
            boolean hasChanges,
            Set<Field> updatedFields
    ) {
        public FetcherResult {
            updatedFields = Set.copyOf(updatedFields);
        }
    }

    public Optional<FetcherResult> fetchEntry(BibEntry entryFromLibrary) {
        if (entryFromLibrary == null) {
            return Optional.empty();
        }

        logEntryProcessing(entryFromLibrary);
        return findFirstValidFetch(entryFromLibrary);
    }

    private void logEntryProcessing(BibEntry entry) {
        LOGGER.debug("Processing library entry: {}",
                entry.getCitationKey().orElse("[no key]"));

        SUPPORTED_FIELDS.forEach(field ->
                entry.getField(field).ifPresent(value ->
                        LOGGER.debug("Entry has {} identifier: {}", field, value)));
    }

    private Optional<FetcherResult> findFirstValidFetch(BibEntry entry) {
        return SUPPORTED_FIELDS.stream()
                               .map(field -> fetchWithField(entry, field))
                               .flatMap(Optional::stream)
                               .findFirst();
    }

    private Optional<FetcherResult> fetchWithField(BibEntry entry, Field field) {
        return entry.getField(field)
                    .flatMap(identifier -> fetchWithIdentifier(field, identifier, entry));
    }

    private Optional<FetcherResult> fetchWithIdentifier(Field field, String identifier,
                                                        BibEntry entryFromLibrary) {
        return WebFetchers.getIdBasedFetcherForField(field, importFormatPreferences)
                          .flatMap(fetcher -> executeFetch(fetcher, field, identifier, entryFromLibrary));
    }

    private Optional<FetcherResult> executeFetch(IdBasedFetcher fetcher, Field field,
                                                 String identifier, BibEntry entryFromLibrary) {
        try {
            LOGGER.debug("Fetching with {}: {}",
                    fetcher.getClass().getSimpleName(), identifier);
            return fetcher.performSearchById(identifier)
                          .map(fetchedEntry -> mergeBibEntries(entryFromLibrary, fetchedEntry));
        } catch (FetcherException e) {
            LOGGER.error("Fetch failed for {} with identifier: {}",
                    field, identifier, e);
            return Optional.empty();
        }
    }

    private FetcherResult mergeBibEntries(BibEntry entryFromLibrary,
                                          BibEntry fetchedEntry) {
        BibEntry mergedEntry = new BibEntry(entryFromLibrary.getType());

        entryFromLibrary.getFields().forEach(field ->
                entryFromLibrary.getField(field)
                                .ifPresent(value -> mergedEntry.setField(field, value)));

        Set<Field> updatedFields = updateFieldsFromSource(fetchedEntry, mergedEntry);

        return new FetcherResult(entryFromLibrary, mergedEntry,
                !updatedFields.isEmpty(), updatedFields);
    }

    private Set<Field> updateFieldsFromSource(BibEntry sourceEntry,
                                              BibEntry targetEntry) {
        return sourceEntry.getFields().stream()
                          .filter(field -> shouldUpdateField(field, sourceEntry, targetEntry))
                          .peek(field -> updateField(field, sourceEntry, targetEntry))
                          .collect(Collectors.toSet());
    }

    private boolean shouldUpdateField(Field field, BibEntry sourceEntry, BibEntry targetEntry) {
        String sourceValue = sourceEntry.getField(field)
                                        .map(REMOVE_WHITESPACE_FORMATTER::format)
                                        .orElse("");
        String targetValue = targetEntry.getField(field)
                                        .map(REMOVE_WHITESPACE_FORMATTER::format)
                                        .orElse("");
        return !sourceValue.equals(targetValue);
    }

    private void updateField(Field field, BibEntry sourceEntry,
                             BibEntry targetEntry) {
        sourceEntry.getField(field).ifPresent(value -> {
            targetEntry.setField(field, value);
            LOGGER.debug("Updated field {}: '{}'", field, value);
        });
    }
}

