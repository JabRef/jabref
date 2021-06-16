package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.support.DisabledOnCIServer;
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
@DisabledOnCIServer("Produces to many requests on CI")
public class CompositeSearchBasedFetcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSearchBasedFetcherTest.class);

    @Test
    public void createCompositeFetcherWithNullSet() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new CompositeSearchBasedFetcher(null, 0));
    }

    @Test
    public void performSearchWithoutFetchers() throws Exception {
        Set<SearchBasedFetcher> empty = new HashSet<>();
        CompositeSearchBasedFetcher fetcher = new CompositeSearchBasedFetcher(empty, Integer.MAX_VALUE);

        List<BibEntry> result = fetcher.performSearch("quantum");

        Assertions.assertEquals(result, Collections.EMPTY_LIST);
    }

    @ParameterizedTest(name = "Perform Search on empty query.")
    @MethodSource("performSearchParameters")
    public void performSearchOnEmptyQuery(Set<SearchBasedFetcher> fetchers) throws Exception {
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers, Integer.MAX_VALUE);

        List<BibEntry> queryResult = compositeFetcher.performSearch("");

        Assertions.assertEquals(queryResult, Collections.EMPTY_LIST);
    }

    @ParameterizedTest(name = "Perform search on query \"quantum\". Using the CompositeFetcher of the following " +
            "Fetchers: {arguments}")
    @MethodSource("performSearchParameters")
    public void performSearchOnNonEmptyQuery(Set<SearchBasedFetcher> fetchers) throws Exception {
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers, Integer.MAX_VALUE);
        ImportCleanup cleanup = new ImportCleanup(BibDatabaseMode.BIBTEX);

        List<BibEntry> compositeResult = compositeFetcher.performSearch("quantum");
        for (SearchBasedFetcher fetcher : fetchers) {
            try {
                List<BibEntry> fetcherResult = fetcher.performSearch("quantum");
                fetcherResult.forEach(cleanup::doPostCleanup);
                Assertions.assertTrue(compositeResult.containsAll(fetcherResult));
            } catch (FetcherException e) {
                /* We catch the Fetcher exception here, since the failing fetcher also fails in the CompositeFetcher
                 * and just leads to no additional results in the returned list. Therefore the test should not fail
                 * due to the fetcher exception
                 */
                LOGGER.debug(String.format("Fetcher %s failed ", fetcher.getName()), e);
            }
        }
    }

    /**
     * This method provides other methods with different sized sets of search-based fetchers wrapped in arguments.
     *
     * @return A stream of Arguments wrapping set of fetchers.
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
        /* Disabled due to an issue regarding comparison: Title fields of the entries that otherwise are equivalent differ
         * due to different JAXBElements.
         */
        // list.add(new MedlineFetcher());

        // Create different sized sets of fetchers to use in the composite fetcher.
        // Selected 1173 to have differencing sets
        for (int i = 1; i < Math.pow(2, list.size()); i += 1173) {
            Set<SearchBasedFetcher> fetchers = new HashSet<>();
            // Only shift i at maximum to its MSB to the right
            for (int j = 0; Math.pow(2, j) <= i; j++) {
                // Add fetcher j to the list if the j-th bit of i is 1
                if ((i >> j) % 2 == 1) {
                    fetchers.add(list.get(j));
                }
            }
            fetcherParameters.add(fetchers);
        }

        return fetcherParameters.stream().map(Arguments::of);
    }
}
