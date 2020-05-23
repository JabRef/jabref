package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class CompositeSearchBasedFetcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSearchBasedFetcherTest.class);

    @Test
    public void performSearchWithoutFetchers() {
        Set<SearchBasedFetcher> empty = new HashSet<>();
        CompositeSearchBasedFetcher fetcher = new CompositeSearchBasedFetcher(empty);

        List<BibEntry> result = fetcher.performSearch("quantum");

        Assertions.assertTrue(result.isEmpty());
    }

    @ParameterizedTest(name = "Perform Search on empty query.")
    @MethodSource("performSearchParameters")
    public void performSearchOnEmptyQuery(Set<SearchBasedFetcher> fetchers) {
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers);

        List<BibEntry> queryResult = compositeFetcher.performSearch("");

        Assertions.assertTrue(queryResult.isEmpty());
    }

    @ParameterizedTest(name = "Perform search on query \"quantum\". Using the CompositeFetcher of the following " +
            "Fetchers: {arguments}")
    @MethodSource("performSearchParameters")
    public void performSearchOnNonEmptyQuery(Set<SearchBasedFetcher> fetchers) {
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers);

        List<BibEntry> compositeResult = compositeFetcher.performSearch("quantum");

        for (SearchBasedFetcher fetcher : fetchers) {
            try {
                Assertions.assertTrue(compositeResult.containsAll(fetcher.performSearch("quantum")));
            } catch (FetcherException e) {
                /* We catch the Fetcher exception here, since the failing fetcher also fails in the CompositeFetcher
                 * and just leads to no additional results in the returned list. Therefor the test should not fail
                 * due to the fetcher exception
                 */
                LOGGER.warn(String.format("Fetcher %s failed ", fetcher.getName()), e);
            }
        }
    }

    /**
     * This method provides other methods with different sized sets of Searchbased fetchers wrapped in Arguments.
     *
     * @return A stream of Arguments wrapping the sets.
     */
    static Stream<Arguments> performSearchParameters() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences())
                .thenReturn(mock(FieldContentFormatterPreferences.class));
        List<Set<SearchBasedFetcher>> fetcherParameters = new ArrayList<>();
        List<SearchBasedFetcher> list = new ArrayList<>();

        list.add(new ArXiv(importFormatPreferences));
        list.add(new INSPIREFetcher(importFormatPreferences));
        list.add(new GvkFetcher());
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.add(new ZbMATH(importFormatPreferences));
        list.add(new GoogleScholar(importFormatPreferences));
        list.add(new DBLPFetcher(importFormatPreferences));
        list.add(new SpringerFetcher());
        list.add(new CrossRef());
        list.add(new CiteSeer());
        list.add(new DOAJFetcher(importFormatPreferences));
        list.add(new IEEE(importFormatPreferences));
        list.add(new GrobidCitationFetcher(importFormatPreferences));
        /* Disabled due to issue regarding Comparison: Title fields of the entries that otherwise are equivalent differ
         * due to different JAXBElements.
         */
        // list.add(new MedlineFetcher());

        // Create different sized sets of fetchers to use in the composite fetcher.
        for (int i = 1; i < list.size(); i++) {
            fetcherParameters.add(new HashSet<>(list.subList(0, i)));
        }

        return fetcherParameters.stream().map(Arguments::of);
    }
}
