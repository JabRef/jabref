package org.jabref.logic.importer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
class IdentifierBasedEntryFetcherTest {

    @Test
    void fetchByFieldNormalizesInfoDoiBeforeFetching() {
        AtomicReference<String> fetchedIdentifier = new AtomicReference<>();
        BibEntry fetchedEntry = new BibEntry().withField(StandardField.TITLE, "Fetched");

        IdentifierBasedEntryFetcher fetcher = new IdentifierBasedEntryFetcher((field, identifier) -> {
            fetchedIdentifier.set(identifier);
            return Optional.of(fetchedEntry);
        });

        BibEntry candidate = new BibEntry().withField(StandardField.DOI, "info:doi/10.1145/3651640.3651646");

        Optional<BibEntry> result = fetcher.fetchByField(List.of(candidate), StandardField.DOI);

        assertEquals(Optional.of(fetchedEntry), result);
        assertEquals("10.1145/3651640.3651646", fetchedIdentifier.get());
    }

    @Test
    void fetchByFieldReturnsEmptyIfNoIdentifierPresent() {
        IdentifierBasedEntryFetcher fetcher = new IdentifierBasedEntryFetcher((field, identifier) -> Optional.of(new BibEntry()));

        Optional<BibEntry> result = fetcher.fetchByField(List.of(new BibEntry()), StandardField.DOI);

        assertEquals(Optional.empty(), result);
    }

    @Test
    void fetchByFieldsReturnsFetchedEntriesForAvailableIdentifiers() {
        IdentifierBasedEntryFetcher fetcher = new IdentifierBasedEntryFetcher((field, identifier) -> Optional.of(new BibEntry().withField(StandardField.TITLE, field.getName() + ":" + identifier)));

        BibEntry candidate = new BibEntry()
                .withField(StandardField.DOI, "10.1145/3651640.3651646")
                .withField(StandardField.ISBN, "9780134685991");

        Map<Field, BibEntry> fetchedEntries = fetcher.fetchByFields(List.of(candidate), List.of(StandardField.DOI, StandardField.ISBN, StandardField.ISSN));

        assertEquals(2, fetchedEntries.size());
        assertEquals(Optional.of("doi:10.1145/3651640.3651646"), Optional.ofNullable(fetchedEntries.get(StandardField.DOI))
                                                                                .flatMap(entry -> entry.getField(StandardField.TITLE)));
        assertEquals(Optional.of("isbn:9780134685991"), Optional.ofNullable(fetchedEntries.get(StandardField.ISBN))
                                                                               .flatMap(entry -> entry.getField(StandardField.TITLE)));
        assertFalse(fetchedEntries.containsKey(StandardField.ISSN));
    }
}
