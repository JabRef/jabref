package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
@DisabledOnCIServer("Produces too many requests on CI")
class CompositeSearchBasedFetcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSearchBasedFetcherTest.class);

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @Test
    void createCompositeFetcherWithNullSet() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompositeSearchBasedFetcher(null, importerPreferences, 0));
    }

    @Test
    void performSearchWithoutFetchers() throws FetcherException {
        Set<SearchBasedFetcher> empty = new HashSet<>();
        CompositeSearchBasedFetcher fetcher = new CompositeSearchBasedFetcher(empty, importerPreferences, Integer.MAX_VALUE);

        List<BibEntry> result = fetcher.performSearch("quantum");

        assertEquals(result, List.of());
    }

    @ParameterizedTest(name = "Perform Search on empty query.")
    @MethodSource("performSearchParameters")
    void performSearchOnEmptyQuery(Set<SearchBasedFetcher> fetchers) throws FetcherException {
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers, importerPreferences, Integer.MAX_VALUE);

        List<BibEntry> queryResult = compositeFetcher.performSearch("");

        assertEquals(queryResult, List.of());
    }

    @ParameterizedTest(name = "Perform search on query \"quantum\". Using the CompositeFetcher of the following " +
            "Fetchers: {arguments}")
    @MethodSource("performSearchParameters")
    void performSearchOnNonEmptyQuery(Set<SearchBasedFetcher> fetchers) throws FetcherException {
        List<String> fetcherNames = fetchers.stream().map(WebFetcher::getName).toList();
        ObservableList<String> observableList = FXCollections.observableArrayList(fetcherNames);
        when(importerPreferences.getCatalogs()).thenReturn(observableList);
        CompositeSearchBasedFetcher compositeFetcher = new CompositeSearchBasedFetcher(fetchers, importerPreferences, Integer.MAX_VALUE);
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        ImportCleanup cleanup = ImportCleanup.targeting(BibDatabaseMode.BIBTEX, fieldPreferences);

        List<BibEntry> compositeResult = compositeFetcher.performSearch("quantum");
        compositeResult.forEach(cleanup::doPostCleanup);
        for (SearchBasedFetcher fetcher : fetchers) {
            try {
                List<BibEntry> fetcherResult = fetcher.performSearch("quantum");
                fetcherResult.forEach(cleanup::doPostCleanup);
                assertTrue(compositeResult.containsAll(fetcherResult), "Did not contain " + fetcherResult);
            } catch (FetcherException e) {
                /* We catch the Fetcher exception here, since the failing fetcher also fails in the CompositeFetcher
                 * and just leads to no additional results in the returned list. Therefore, the test should not fail
                 * due to the fetcher exception
                 */
                LOGGER.debug("Fetcher {} failed ", fetcher.getName(), e);
            }
        }
    }

    /**
     * This method provides other methods with different sized sets of search-based fetchers wrapped in arguments.
     *
     * @return A stream of Arguments wrapping set of fetchers.
     */
    static Stream<Arguments> performSearchParameters() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        BuildInfo buildInfo = new BuildInfo();
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(eq(AstrophysicsDataSystem.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.astrophysicsDataSystemAPIKey));
        when(importerPreferences.getApiKey(eq(SemanticScholarCitationFetcher.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.semanticScholarApiKey));
        when(importerPreferences.getApiKey(eq(BiodiversityLibrary.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.biodiversityHeritageApiKey));
        when(importerPreferences.getApiKey(eq(Scopus.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.scopusApiKey));
        when(importerPreferences.getApiKey(eq(SpringerNatureWebFetcher.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.springerNatureAPIKey));
        when(importerPreferences.getApiKey(eq(IEEE.FETCHER_NAME))).thenReturn(Optional.of(buildInfo.ieeeAPIKey));

        List<Set<SearchBasedFetcher>> fetcherParameters = new ArrayList<>();

        List<SearchBasedFetcher> list = List.of(
                new ArXivFetcher(importFormatPreferences),
                new INSPIREFetcher(importFormatPreferences),
                new GvkFetcher(importFormatPreferences),
                new AstrophysicsDataSystem(importFormatPreferences, importerPreferences),
                new MathSciNet(importFormatPreferences),
                new ZbMATH(importFormatPreferences),
                new GoogleScholar(importFormatPreferences),
                new DBLPFetcher(importFormatPreferences),
                new SpringerNatureWebFetcher(importerPreferences),
                new CrossRef(),
                new CiteSeer(),
                new DOAJFetcher(importFormatPreferences),
                new IEEE(importFormatPreferences, importerPreferences),
                new Scopus(importerPreferences));

        /* Disabled due to an issue regarding comparison: Title fields of the entries that otherwise are equivalent differ
         * due to different JAXBElements.
         */
        // new MedlineFetcher()

        // Create different sized sets of fetchers to use in the composite fetcher.
        // Selected 1173 to have differencing sets
        for (int i = 1; i < Math.pow(2, list.size()); i += 1173) {
            Set<SearchBasedFetcher> fetchers = new HashSet<>();
            // Only shift i at maximum to its MSB to the right
            for (int j = 0; Math.pow(2, j) <= i; j++) {
                // Add fetcher j to the list if the j-th bit of i is 1
                if (((i >> j) % 2) == 1) {
                    fetchers.add(list.get(j));
                }
            }
            fetcherParameters.add(fetchers);
        }

        return fetcherParameters.stream().map(Arguments::of);
    }
}
