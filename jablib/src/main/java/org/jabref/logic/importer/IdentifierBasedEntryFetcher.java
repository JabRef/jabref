package org.jabref.logic.importer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetches bibliographic entries based on identifiers found in already parsed {@link BibEntry} candidates.
public class IdentifierBasedEntryFetcher {

    public static final List<Field> SUPPORTED_FIELDS = List.of(
            StandardField.DOI,
            StandardField.EPRINT,
            StandardField.ISBN,
            StandardField.ISSN
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifierBasedEntryFetcher.class);

    private final BiFunction<Field, String, Optional<BibEntry>> fetchByFieldAndIdentifier;

    public IdentifierBasedEntryFetcher(ImportFormatPreferences importFormatPreferences) {
        this((field, identifier) -> WebFetchers.getIdBasedFetcherForField(field, importFormatPreferences)
                                               .flatMap(fetcher -> {
                                                   try {
                                                       return fetcher.performSearchById(identifier);
                                                   } catch (FetcherException e) {
                                                       LOGGER.debug("Could not fetch entry by {} '{}'.", field, identifier, e);
                                                       return Optional.empty();
                                                   }
                                               }));
    }

    IdentifierBasedEntryFetcher(BiFunction<Field, String, Optional<BibEntry>> fetchByFieldAndIdentifier) {
        this.fetchByFieldAndIdentifier = fetchByFieldAndIdentifier;
    }

    public Optional<BibEntry> fetchByField(List<BibEntry> candidates, Field field) {
        return findIdentifier(candidates, field)
                .flatMap(identifier -> fetchByFieldAndIdentifier.apply(field, normalizeIdentifier(field, identifier)));
    }

    public Map<Field, BibEntry> fetchByFields(List<BibEntry> candidates, List<Field> fields) {
        Map<Field, BibEntry> fetchedEntries = new LinkedHashMap<>();

        for (Field field : fields) {
            fetchByField(candidates, field).ifPresent(entry -> fetchedEntries.put(field, entry));
        }

        return fetchedEntries;
    }

    private Optional<String> findIdentifier(List<BibEntry> candidates, Field field) {
        for (BibEntry candidate : candidates) {
            Optional<String> identifier = candidate.getField(field);
            if (identifier.isPresent()) {
                return identifier;
            }
        }

        return Optional.empty();
    }

    static String normalizeIdentifier(Field field, String identifier) {
        String trimmedIdentifier = identifier.trim();

        if (field == StandardField.DOI) {
            String cleanedIdentifier = trimmedIdentifier.replaceFirst("(?i)^info:doi[:/]", "");
            return DOI.parse(cleanedIdentifier)
                      .map(DOI::asString)
                      .orElse(cleanedIdentifier);
        }

        return trimmedIdentifier;
    }
}
