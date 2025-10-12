package org.jabref.logic.citation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepository;
import org.jabref.logic.citation.repository.BibEntryRelationsRepositoryTestHelpers;
import org.jabref.logic.importer.FetcherException;
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
    private CitationFetcher createMockFetcher(BibEntry targetEntry, List<BibEntry> citationsToReturn, List<BibEntry> referencesToReturn, Integer citationCount) {
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
                },
                entry -> {
                    if (entry == targetEntry) {
                        return Optional.of(citationCount);
                    }
                    return Optional.empty();
                }
        );
    }

    /**
     * Creates a mock CitationFetcher that returns empty lists for all entries
     */
    private CitationFetcher createEmptyMockFetcher() {
        return CitationFetcherHelpersForTest.Mocks.from(
                _ -> List.of(),
                _ -> List.of(),
                _ -> Optional.empty()
        );
    }

    @Nested
    class CitationsTests {
        @Test
        void serviceShouldSearchForCitations() throws FetcherException {
            // GIVEN
            BibEntry cited = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(new BibEntry());
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    _ -> citationsToReturn, null, null, null, _ -> false, _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitedBy(cited);

            // THEN
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldCallTheFetcherForCitationsWhenRepositoryIsUpdatable() throws FetcherException {
            // GiVEN
            BibEntry cited = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> citationsDatabase = HashMap.newHashMap(300);
            CitationFetcher fetcher = createMockFetcher(cited, citationsToReturn, null, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    _ -> citationsToReturn,
                    citationsDatabase::put,
                    List::of,
                    (_, _) -> {
                    },
                    _ -> true,
                    _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitedBy(cited);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldFetchCitationsIfRepositoryIsEmpty() throws FetcherException {
            BibEntry cited = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> citationsToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> citationsDatabase = HashMap.newHashMap(300);
            CitationFetcher fetcher = createMockFetcher(cited, citationsToReturn, null, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(citationsDatabase, null, true);
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitedBy(cited);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void insertingAnEmptyCitationsShouldBePossible() throws FetcherException {
            BibEntry cited = new BibEntry();
            Map<BibEntry, List<BibEntry>> citationsDatabase = new HashMap<>();
            CitationFetcher fetcher = createEmptyMockFetcher();
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(citationsDatabase, null, true);
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitedBy(cited);

            // THEN
            assertTrue(citations.isEmpty());
            assertTrue(citationsDatabase.containsKey(cited));
            assertTrue(citationsDatabase.get(cited).isEmpty());
        }
    }

    @Nested
    class ReferencesTests {
        @Test
        void serviceShouldSearchForReferences() throws FetcherException {
            // GIVEN
            BibEntry referencer = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(new BibEntry());
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, null, _ -> referencesToReturn, null, _ -> false, _ -> false
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> references = searchService.searchCites(referencer);

            // THEN
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldCallTheFetcherForReferencesWhenRepositoryIsUpdatable() throws FetcherException {
            // GIVEN
            BibEntry referencer = new BibEntry();
            BibEntry newReference = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(newReference);
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            CitationFetcher fetcher = createMockFetcher(referencer, null, referencesToReturn, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    List::of,
                    (_, _) -> {
                    },
                    _ -> referencesToReturn,
                    referencesDatabase::put,
                    _ -> false,
                    _ -> true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> references = searchService.searchCites(referencer);

            // THEN
            assertTrue(referencesDatabase.containsKey(referencer));
            assertEquals(referencesToReturn, referencesDatabase.get(referencer));
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldFetchReferencesIfRepositoryIsEmpty() throws FetcherException {
            BibEntry reference = new BibEntry();
            BibEntry newCitations = new BibEntry();
            List<BibEntry> referencesToReturn = List.of(newCitations);
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            CitationFetcher fetcher = createMockFetcher(reference, null, referencesToReturn, null);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referencesDatabase, true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> references = searchService.searchCites(reference);

            // THEN
            assertTrue(referencesDatabase.containsKey(reference));
            assertEquals(referencesToReturn, referencesDatabase.get(reference));
            assertEquals(referencesToReturn, references);
        }

        @Test
        void insertingAnEmptyReferencesShouldBePossible() throws FetcherException {
            BibEntry referencer = new BibEntry();
            Map<BibEntry, List<BibEntry>> referenceDatabase = new HashMap<>();
            CitationFetcher fetcher = createEmptyMockFetcher();
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referenceDatabase, true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCites(referencer);

            // THEN
            assertTrue(citations.isEmpty());
            assertTrue(referenceDatabase.containsKey(referencer));
            assertTrue(referenceDatabase.get(referencer).isEmpty());
        }

        @Test
        void serviceShouldUpdateCitationCountWithEmptyPaperDetailsResponse() throws FetcherException {
            int expectedResult = 0;
            BibEntry referencer = new BibEntry();
            Map<BibEntry, List<BibEntry>> referenceDatabase = new HashMap<>();
            CitationFetcher fetcher = createEmptyMockFetcher();
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referenceDatabase, true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);
            Optional<String> field = Optional.empty();
            int citationsCount = searchService.getCitationCount(referencer, field);
            assertEquals(citationsCount, expectedResult);
        }

        @Test
        void serviceShouldCorrectlyFetchCitationCountField() throws FetcherException {
            int expectedResult = 3;
            BibEntry reference = new BibEntry();
            Integer citationCount = 3;
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            CitationFetcher fetcher = createMockFetcher(reference, null, null, citationCount);

            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referencesDatabase, true
            );
            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);
            Optional<String> field = Optional.empty();
            int citationsCount = searchService.getCitationCount(reference, field);
            assertEquals(citationsCount, expectedResult);
        }

        @Test
        void serviceShouldUpdateBecauseIsisCitationsUpdatableTrue() throws FetcherException {
            int expectedResult = 3;
            BibEntry reference = new BibEntry();
            Integer citationCount = 3;
            Map<BibEntry, List<BibEntry>> referencesDatabase = new HashMap<>();
            referencesDatabase.put(reference, List.of());

            CitationFetcher fetcher = createMockFetcher(reference, null, null, citationCount);
            BibEntryCitationsAndReferencesRepository repository = BibEntryRelationsRepositoryTestHelpers.Mocks.from(
                    null, referencesDatabase, true
            );

            SearchCitationsRelationsService searchService = new SearchCitationsRelationsService(fetcher, repository);
            Optional<String> field = Optional.empty();
            int citationsCount = searchService.getCitationCount(reference, field);
            assertEquals(citationsCount, expectedResult);
        }
    }
}
