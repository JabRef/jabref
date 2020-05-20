package org.jabref.logic.importer.fetcher;

import java.util.HashSet;
import java.util.Set;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class CompositeSearchBasedFetcherTest {

    private CompositeSearchBasedFetcher finder;
    private SearchBasedFetcher searchBasedFetcher;
    private SearchBasedParserFetcher searchBasedParserFetcher;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);

        Set<SearchBasedFetcher> setOfSearchBasedFetchers = new HashSet<>();
        // Add one searchBasedFetcher and one searchBasedParserFetcher
        searchBasedFetcher = new ArXiv(importFormatPreferences);
        searchBasedParserFetcher = new CiteSeer();
        setOfSearchBasedFetchers.add(searchBasedFetcher);
        setOfSearchBasedFetchers.add(searchBasedParserFetcher);
        finder = new CompositeSearchBasedFetcher(setOfSearchBasedFetchers);
    }

    @Test
    public void performSearchOnEmptyQuery() {
        Assertions.assertTrue(finder.performSearch("").isEmpty());
    }

    @Test
    public void performSearchContainsResultsOfSearchBasedFetcher() throws FetcherException {
        Assertions.assertTrue(finder.performSearch("quantum").containsAll(searchBasedFetcher.performSearch("quantum")));
    }

    @Test
    public void performSearchContainsResultsOfSearchBasedParserFetcher() throws FetcherException {
        Assertions.assertTrue(finder.performSearch("quantum").containsAll(searchBasedParserFetcher.performSearch("quantum")));
    }
}
