package org.jabref.logic.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.study.FetchResult;
import org.jabref.model.study.QueryResult;
import org.jabref.model.study.StudyQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Delegates the search of the provided set of targeted E-Libraries with the provided queries to the E-Library specific fetchers,
/// and aggregates the results returned by the fetchers by query and E-Library.
class StudyFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyFetcher.class);

    private final List<SearchBasedFetcher> activeFetchers;
    private final List<StudyQuery> searchQueries;
    private final Map<String, Integer> resultLimits;

    StudyFetcher(List<SearchBasedFetcher> activeFetchers, List<StudyQuery> searchQueries, Map<String, Integer> resultLimits) {
        this.searchQueries = searchQueries;
        this.activeFetchers = activeFetchers;
        this.resultLimits = resultLimits;
    }

    /// Each Map Entry contains the results for one search term for all libraries.
    /// Each entry of the internal map contains the results for a given library.
    /// If any library API is not available, its corresponding entry is missing from the internal map.
    public List<QueryResult> crawl() {
        return searchQueries.parallelStream()
                            .map(this::getQueryResult)
                            .toList();
    }

    private QueryResult getQueryResult(StudyQuery searchQuery) {
        return new QueryResult(searchQuery.getQuery(), performSearchOnQuery(searchQuery));
    }

    /// Queries all catalogs on the given searchQuery.
    ///
    /// @param searchQuery The query the search is performed for.
    /// @return Mapping of each fetcher by name and all their retrieved publications as a BibDatabase
    private List<FetchResult> performSearchOnQuery(StudyQuery searchQuery) {
        return activeFetchers.parallelStream()
                             .map(fetcher -> performSearchOnQueryForFetcher(searchQuery, fetcher))
                             .flatMap(Optional::stream)
                             .toList();
    }

    private Optional<FetchResult> performSearchOnQueryForFetcher(StudyQuery searchQuery, SearchBasedFetcher fetcher) {
        try {
            Optional<String> catalogOverride = searchQuery.getCatalogSpecific().entrySet().stream()
                                                          .filter(entry -> entry.getKey().equalsIgnoreCase(fetcher.getName()))
                                                          .map(Map.Entry::getValue)
                                                          .filter(v -> v != null && !v.isBlank())
                                                          .findFirst();
            List<BibEntry> fetchResult;
            if (fetcher instanceof PagedSearchBasedFetcher basedFetcher) {
                fetchResult = performPagedSearch(basedFetcher, catalogOverride, searchQuery);
            } else {
                fetchResult = performNonPagedSearch(fetcher, catalogOverride, searchQuery);
            }
            return Optional.of(new FetchResult(fetcher.getName(), new BibDatabase(fetchResult)));
        } catch (FetcherException e) {
            LOGGER.warn("{} API request failed", fetcher.getName(), e);
            return Optional.empty();
        }
    }

    private List<BibEntry> performPagedSearch(PagedSearchBasedFetcher basedFetcher, Optional<String> catalogOverride, StudyQuery searchQuery) throws FetcherException {
        int limit = resultLimits.getOrDefault(basedFetcher.getName(), StudyRepository.DEFAULT_RESULT_LIMIT);
        int pages = (int) Math.ceil((double) limit / basedFetcher.getPageSize());
        List<BibEntry> fetchResult = new ArrayList<>();
        if (catalogOverride.isPresent()) {
            try {
                for (int page = 0; page < pages; page++) {
                    fetchResult.addAll(basedFetcher.performRawSearchQueryPaged(catalogOverride.get(), page).getContent());
                }
            } catch (UnsupportedOperationException e) {
                throw new FetcherException(basedFetcher.getName() + " does not support raw search queries for catalogSpecific override", e);
            }
        } else {
            for (int page = 0; page < pages; page++) {
                fetchResult.addAll(basedFetcher.performSearchPaged(searchQuery.getQuery(), page).getContent());
            }
        }
        if (fetchResult.size() > limit) {
            fetchResult = new ArrayList<>(fetchResult.subList(0, limit));
        }
        return fetchResult;
    }

    private List<BibEntry> performNonPagedSearch(SearchBasedFetcher fetcher, Optional<String> catalogOverride, StudyQuery searchQuery) throws FetcherException {
        if (catalogOverride.isPresent()) {
            try {
                return fetcher.performRawSearchQuery(catalogOverride.get());
            } catch (UnsupportedOperationException e) {
                throw new FetcherException(fetcher.getName() + " does not support raw search queries for catalogSpecific override", e);
            }
        } else {
            return fetcher.performSearch(searchQuery.getQuery());
        }
    }
}
