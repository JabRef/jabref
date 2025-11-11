package org.jabref.logic.citation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepository;
import org.jabref.logic.citation.repository.BibEntryRelationsRepositoryTestHelpers;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherHelpersForTest;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchCitationsRelationsServiceTest {

    /**
     * Creates a mock CitationFetcher that returns specific results for citations and references
     */
    private CitationFetcher createMockFetcher(BibEntry targetEntry, List<BibEntry> citationsToReturn, List<BibEntry> referencesToReturn) {
        return CitationFetcherHelpersForTest.Mocks.from(
                entry -> {
                    if (entry == targetEntry) {
                        return citationsToReturn != null ? citationsToReturn : List.of();
                    }
                    return List.of();
                },
                entry -> {
                    if (entry == targetEntry) {
                        return referencesToReturn != null ? referencesToReturn : List.of();
                    }
                    return List.of();
                }
        );
    }

    /**
     * Creates a mock CitationFetcher that returns empty lists for all entries
     */
    private CitationFetcher createEmptyMockFetcher() {
        return CitationFetcherHelpersForTest.Mocks.from(
                _ -> List.of(),
                _ -> List.of()
        );
    }

    @Nested
    class CitationsTests {
        @Test
        void serviceShouldSearchForCitations() {
            // GIVEN
            BibEntry cited = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(new BibEntry());
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    _ -> citationsToReturn, null, null, null, _ -> false, _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitations(cited);

            // THEN
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldCallTheFetcherForCitationsWhenRepositoryIsUpdatable() {
            // GiVEN
            BibEntry cited = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> citationsDatabase = HashMap.newHashMap(300);
            CitationFetcher fetcher = createMockFetcher(cited, citationsToReturn, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    _ -> citationsToReturn,
                    citationsDatabase::put,
                    List::of,
                    (_, _) -> { },
                    _ -> true,
                    _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitations(cited);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldFetchCitationsIfRepositoryIsEmpty() {
            BibEntry cited = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> citationsDatabase = HashMap.newHashMap(300);
            CitationFetcher fetcher = createMockFetcher(cited, citationsToReturn, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(citationsDatabase, null);
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitations(cited);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void insertingAnEmptyCitationsShouldBePossible() {
            BibEntry cited = new BibEntry();
            Map<BibEntry, List<BibEntry>> citationsDatabase = new HashMap<>();
            CitationFetcher fetcher = createEmptyMockFetcher();
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(citationsDatabase, null);
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitations(cited);

            // THEN
            assertTrue(citations.isEmpty());
            assertTrue(citationsDatabase.containsKey(cited));
            assertTrue(citationsDatabase.get(cited).isEmpty());
        }
    }

    @Nested
    class ReferencesTests {
        @Test
        void serviceShouldSearchForReferences() {
            // GIVEN
            BibEntry referencer = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(new BibEntry());
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, null, _ -> referencesToReturn, null, _ -> false, _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> references = searchService.searchReferences(referencer);

            // THEN
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldCallTheFetcherForReferencesWhenRepositoryIsUpdatable() {
            // GIVEN
            BibEntry referencer = new BibEntry();
            BibEntry newReference = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(newReference);
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            CitationFetcher fetcher = createMockFetcher(referencer, null, referencesToReturn);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    List::of,
                    (_, _) -> { },
                    _ -> referencesToReturn,
                    referencesDatabase::put,
                    _ -> false,
                    _ -> true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> references = searchService.searchReferences(referencer);

            // THEN
            assertTrue(referencesDatabase.containsKey(referencer));
            assertEquals(referencesToReturn, referencesDatabase.get(referencer));
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldFetchReferencesIfRepositoryIsEmpty() {
            BibEntry reference = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            CitationFetcher fetcher = createMockFetcher(reference, null, referencesToReturn);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referencesDatabase
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> references = searchService.searchReferences(reference);

            // THEN
            assertTrue(referencesDatabase.containsKey(reference));
            assertEquals(referencesToReturn, referencesDatabase.get(reference));
            assertEquals(referencesToReturn, references);
        }

        @Test
        void insertingAnEmptyReferencesShouldBePossible() {
            BibEntry referencer = new BibEntry();
            Map<BibEntry, List<BibEntry>> referenceDatabase = new HashMap<>();
            CitationFetcher fetcher = createEmptyMockFetcher();
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referenceDatabase
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchReferences(referencer);

            // THEN
            assertTrue(citations.isEmpty());
            assertTrue(referenceDatabase.containsKey(referencer));
            assertTrue(referenceDatabase.get(referencer).isEmpty());
        }
    }
}
