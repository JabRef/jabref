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
        for (BibEntry candidate : candidates) {
            Optional<String> normalizedIdentifier = candidate.getField(field)
                                                            .map(String::trim)
                                                            .filter(identifier -> !identifier.isBlank())
                                                            .flatMap(identifier -> normalizeIdentifier(field, identifier));

            if (normalizedIdentifier.isEmpty()) {
                continue;
            }

            Optional<BibEntry> fetchedEntry = fetchByFieldAndIdentifier.apply(field, normalizedIdentifier.get());
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        }

        return Optional.empty();
    }

    public Map<Field, BibEntry> fetchByFields(List<BibEntry> candidates, List<Field> fields) {
        Map<Field, BibEntry> fetchedEntries = new LinkedHashMap<>();

        for (Field field : fields) {
            fetchByField(candidates, field).ifPresent(entry -> fetchedEntries.put(field, entry));
        }

        return fetchedEntries;
    }

    static Optional<String> normalizeIdentifier(Field field, String identifier) {
        String trimmedIdentifier = identifier.trim();

        if (field == StandardField.DOI) {
            // Some PDF metadata stores DOI values as info URIs such as `info:doi/10...`
            // so the prefix is stripped before passing the remaining value to JabRef's DOI parser.
            String cleanedIdentifier = trimmedIdentifier.replaceFirst("(?i)^info:doi[:/]", "");
            return DOI.parse(cleanedIdentifier)
                      .map(DOI::asString);
        }

        return Optional.of(trimmedIdentifier);
    }
}
