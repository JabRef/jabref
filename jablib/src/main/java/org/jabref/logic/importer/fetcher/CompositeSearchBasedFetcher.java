package org.jabref.logic.importer.fetcher;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Implements "search-pre configured"
public class CompositeSearchBasedFetcher implements SearchBasedFetcher {

    public static final String FETCHER_NAME = "Search pre-selected";

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSearchBasedFetcher.class);

    private final Set<SearchBasedFetcher> searchBasedFetchers;
    private final ImporterPreferences importerPreferences;
    private final int maximumNumberOfReturnedResults;

    /// @param searchBasedFetchers all available search-based fetchers
    @NullMarked
    public CompositeSearchBasedFetcher(Set<SearchBasedFetcher> searchBasedFetchers, ImporterPreferences importerPreferences, int maximumNumberOfReturnedResults) {
        this.searchBasedFetchers = Set.copyOf(searchBasedFetchers);
        this.importerPreferences = importerPreferences;
        this.maximumNumberOfReturnedResults = maximumNumberOfReturnedResults;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    @Override
    public List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException {
        Collection<String> catalogs = importerPreferences.getCatalogs();
        return searchBasedFetchers.parallelStream()
                                  // Removal the Composite Fetcher instance from its own fetcher set is not required any more as the constructor stores a copy of the set.
                                  // .filter(searchBasedFetcher -> searchBasedFetcher != this)
                                  // Remove any unselected Fetcher instance
                                  .filter(searchBasedFetcher -> catalogs.stream()
                                                                        .anyMatch(name -> name.equals(searchBasedFetcher.getName())))
                                  .flatMap(searchBasedFetcher -> {
                                      try {
                                          // All entries have to be converted before into one format, this is necessary for the format conversion
                                          return searchBasedFetcher.performSearch(queryList).stream();
                                      } catch (FetcherException e) {
                                          LOGGER.warn("{} API request failed", searchBasedFetcher.getName(), e);
                                          return Stream.empty();
                                      }
                                  })
                                  .limit(maximumNumberOfReturnedResults)
                                  .toList();
    }
}
