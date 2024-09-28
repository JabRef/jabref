package org.jabref.logic.citation.service;

import java.util.HashMap;
import java.util.List;

import org.jabref.logic.citation.repository.BibEntryRelationsRepositoryHelpersForTest;
import org.jabref.logic.importer.fetcher.CitationFetcherHelpersForTest;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchCitationsRelationsServiceTest {

    @Nested
    class CitationsTests {
        @Test
        void serviceShouldSearchForCitations() {
            // GIVEN
            var cited = new BibEntry();
            var citationsToReturn = List.of(new BibEntry());
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                e -> citationsToReturn, null, null, null
            );
            var searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> citations = searchService.searchCitations(cited, false);

            // THEN
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldForceCitationsUpdate() {
            // GiVEN
            var cited = new BibEntry();
            var newCitations = new BibEntry();
            var citationsToReturn = List.of(newCitations);
            var citationsDatabase = new HashMap<BibEntry, List<BibEntry>>();
            var fetcher = CitationFetcherHelpersForTest.Mocks.from(
                entry -> {
                    if (entry == cited) {
                        return citationsToReturn;
                    }
                    return List.of();
                },
                null
            );
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                e -> citationsToReturn,
                citationsDatabase::put,
                List::of,
                (e, r) -> { }
            );
            var searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            var citations = searchService.searchCitations(cited, true);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }

        @Test
        void serviceShouldFetchCitationsIfRepositoryIsEmpty() {
            var cited = new BibEntry();
            var newCitations = new BibEntry();
            var citationsToReturn = List.of(newCitations);
            var citationsDatabase = new HashMap<BibEntry, List<BibEntry>>();
            var fetcher = CitationFetcherHelpersForTest.Mocks.from(
                entry -> {
                    if (entry == cited) {
                        return citationsToReturn;
                    }
                    return List.of();
                },
                null
            );
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                citationsDatabase, null
            );
            var searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            var citations = searchService.searchCitations(cited, false);

            // THEN
            assertTrue(citationsDatabase.containsKey(cited));
            assertEquals(citationsToReturn, citationsDatabase.get(cited));
            assertEquals(citationsToReturn, citations);
        }
    }

    @Nested
    class ReferencesTests {
        @Test
        void serviceShouldSearchForReferences() {
            // GIVEN
            var referencer = new BibEntry();
            var referencesToReturn = List.of(new BibEntry());
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                null, null, e -> referencesToReturn, null
            );
            var searchService = new SearchCitationsRelationsService(null, repository);

            // WHEN
            List<BibEntry> references = searchService.searchReferences(referencer, false);

            // THEN
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldCallTheFetcherForReferencesIWhenForceUpdateIsTrue() {
            // GIVEN
            var referencer = new BibEntry();
            var newReference = new BibEntry();
            var referencesToReturn = List.of(newReference);
            var referencesDatabase = new HashMap<BibEntry, List<BibEntry>>();
            var fetcher = CitationFetcherHelpersForTest.Mocks.from(null, entry -> {
                if (entry == referencer) {
                    return referencesToReturn;
                }
                return List.of();
            });
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                List::of,
                (e, c) -> { },
                e -> referencesToReturn,
                referencesDatabase::put
            );
            var searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            var references = searchService.searchReferences(referencer, true);

            // THEN
            assertTrue(referencesDatabase.containsKey(referencer));
            assertEquals(referencesToReturn, referencesDatabase.get(referencer));
            assertEquals(referencesToReturn, references);
        }

        @Test
        void serviceShouldFetchReferencesIfRepositoryIsEmpty() {
            var reference = new BibEntry();
            var newCitations = new BibEntry();
            var referencesToReturn = List.of(newCitations);
            var referencesDatabase = new HashMap<BibEntry, List<BibEntry>>();
            var fetcher = CitationFetcherHelpersForTest.Mocks.from(
                null,
                entry -> {
                    if (entry == reference) {
                        return referencesToReturn;
                    }
                    return List.of();
                }
            );
            var repository = BibEntryRelationsRepositoryHelpersForTest.Mocks.from(
                null, referencesDatabase
            );
            var searchService = new SearchCitationsRelationsService(fetcher, repository);

            // WHEN
            var references = searchService.searchReferences(reference, false);

            // THEN
            assertTrue(referencesDatabase.containsKey(reference));
            assertEquals(referencesToReturn, referencesDatabase.get(reference));
            assertEquals(referencesToReturn, references);
        }
    }
}
