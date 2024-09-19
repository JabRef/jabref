package org.jabref.logic.citation.repository;

import java.util.HashSet;
import java.util.List;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CitationFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BibEntryRelationsRepositoryTest {

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(BibEntryRelationsRepositoryTest::createBibEntry);
    }

    private static List<BibEntry> getCitedBy(BibEntry entry) {
        return List.of(BibEntryRelationsRepositoryTest.createCitingBibEntry(entry));
    }

    private static BibEntry createBibEntry(int i) {
        return new BibEntry()
                .withCitationKey("entry" + i)
                .withField(StandardField.DOI, "10.1234/5678" + i);
    }

    private static BibEntry createCitingBibEntry(Integer i) {
        return new BibEntry()
                .withCitationKey("citing_entry" + i)
                .withField(StandardField.DOI, "10.2345/6789" + i);
    }

    private static BibEntry createCitingBibEntry(BibEntry citedEntry) {
        return createCitingBibEntry(
            Integer.valueOf(citedEntry.getCitationKey().orElseThrow().substring(5))
        );
    }

    /**
     * Simple mock to avoid using Mockito (reduce overall complexity)
     */
    private record CitationFetcherMock(
        Function<BibEntry, List<BibEntry>> searchCiteByDelegate,
        Function<BibEntry, List<BibEntry>> searchCitingDelegate,
        String name
    ) implements CitationFetcher {

        @Override
        public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
            return this.searchCiteByDelegate.apply(entry);
        }

        @Override
        public List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
            return this.searchCitingDelegate.apply(entry);
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    @DisplayName(
        "Given a new bib entry when reading citations for it should call the fetcher"
    )
    void givenANewEntryWhenReadingCitationsForItShouldCallTheFetcher(BibEntry bibEntry) {
        // GIVEN
        var entryCaptor = new HashSet<BibEntry>();
        var citationFetcherMock = new CitationFetcherMock(
            entry -> {
                entryCaptor.add(entry);
                return BibEntryRelationsRepositoryTest.getCitedBy(entry);
            },
            null,
            null
        );
        var bibEntryRelationsCache = new BibEntryRelationsCache();
        var bibEntryRelationsRepository = new BibEntryRelationsRepository(
            citationFetcherMock, bibEntryRelationsCache
        );

        // WHEN
        var citations = bibEntryRelationsRepository.getCitations(bibEntry);

        // THEN
        Assertions.assertFalse(citations.isEmpty());
        Assertions.assertTrue(entryCaptor.contains(bibEntry));
    }

    @Test
    @DisplayName(
        "Given an empty cache for a valid entry when reading the citations should populate cache"
    )
    void givenAnEmptyCacheAndAValidBibEntryWhenReadingCitationsShouldPopulateTheCache() {
        // GIVEN
        var citationFetcherMock = new CitationFetcherMock(
            BibEntryRelationsRepositoryTest::getCitedBy, null, null
        );
        var bibEntryRelationsCache = new BibEntryRelationsCache();
        var bibEntryRelationsRepository = new BibEntryRelationsRepository(
            citationFetcherMock, bibEntryRelationsCache
        );
        var bibEntry = BibEntryRelationsRepositoryTest.createBibEntry(1);

        // WHEN
        Assertions.assertEquals(List.of(), bibEntryRelationsCache.getCitations(bibEntry));
        var citations = bibEntryRelationsRepository.getCitations(bibEntry);
        var fromCacheCitations = bibEntryRelationsCache.getCitations(bibEntry);

        // THEN
        Assertions.assertFalse(fromCacheCitations.isEmpty());
        Assertions.assertEquals(citations, fromCacheCitations);
    }
}
