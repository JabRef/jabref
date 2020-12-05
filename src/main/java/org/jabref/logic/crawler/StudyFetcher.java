package org.jabref.logic.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.study.FetchResult;
import org.jabref.model.study.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates the search of the provided set of targeted E-Libraries with the provided queries to the E-Library specific fetchers,
 * and aggregates the results returned by the fetchers by query and E-Library.
 */
class StudyFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyFetcher.class);
    private static final int MAX_AMOUNT_OF_RESULTS_PER_FETCHER = 100;

    private final List<SearchBasedFetcher> activeFetchers;
    private final List<String> searchQueries;

    StudyFetcher(List<SearchBasedFetcher> activeFetchers, List<String> searchQueries) throws IllegalArgumentException {
        this.searchQueries = searchQueries;
        this.activeFetchers = activeFetchers;
    }

    /**
     * Each Map Entry contains the results for one search term for all libraries.
     * Each entry of the internal map contains the results for a given library.
     * If any library API is not available, its corresponding entry is missing from the internal map.
     */
    public List<QueryResult> crawl() {
        return searchQueries.parallelStream()
                            .map(this::getQueryResult)
                            .collect(Collectors.toList());
    }

    private QueryResult getQueryResult(String searchQuery) {
        return new QueryResult(searchQuery, performSearchOnQuery(searchQuery));
    }

    /**
     * Queries all Databases on the given searchQuery.
     *
     * @param searchQuery The query the search is performed for.
     * @return Mapping of each fetcher by name and all their retrieved publications as a BibDatabase
     */
    private List<FetchResult> performSearchOnQuery(String searchQuery) {
        return activeFetchers.parallelStream()
                             .map(fetcher -> performSearchOnQueryForFetcher(searchQuery, fetcher))
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    private FetchResult performSearchOnQueryForFetcher(String searchQuery, SearchBasedFetcher fetcher) {
        try {
            List<BibEntry> fetchResult = new ArrayList<>();
            if (fetcher instanceof PagedSearchBasedFetcher) {
                int pages = ((int) Math.ceil(((double) MAX_AMOUNT_OF_RESULTS_PER_FETCHER) / ((PagedSearchBasedFetcher) fetcher).getPageSize()));
                for (int page = 0; page < pages; page++) {
                    fetchResult.addAll(((PagedSearchBasedFetcher) fetcher).performSearchPaged(searchQuery, page).getContent());
                }
            } else {
                fetchResult = fetcher.performSearch(searchQuery);
            }
            return new FetchResult(fetcher.getName(), new BibDatabase(fetchResult));
        } catch (FetcherException e) {
            LOGGER.warn(String.format("%s API request failed", fetcher.getName()), e);
            return null;
        }
    }
}
