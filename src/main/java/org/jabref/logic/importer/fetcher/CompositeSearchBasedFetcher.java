package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeSearchBasedFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSearchBasedFetcher.class);

    private final Set<SearchBasedFetcher> fetchers;
    private final int maximumNumberOfReturnedResults;

    public CompositeSearchBasedFetcher(Set<SearchBasedFetcher> searchBasedFetchers, int maximumNumberOfReturnedResults)
            throws IllegalArgumentException {
        if (searchBasedFetchers == null) {
            throw new IllegalArgumentException("The set of searchBasedFetchers must not be null!");
        }
        // Remove the Composite Fetcher instance from its own fetcher set to prevent a StackOverflow
        this.fetchers = searchBasedFetchers.stream()
                                           .filter(searchBasedFetcher -> searchBasedFetcher != this)
                                           .collect(Collectors.toSet());
        this.maximumNumberOfReturnedResults = maximumNumberOfReturnedResults;
    }

    @Override
    public String getName() {
        return "SearchAll";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        // All entries have to be converted into one format, this is necessary for the format conversion
        return fetchers.parallelStream()
                       .flatMap(searchBasedFetcher -> {
                           try {
                               return searchBasedFetcher.performSearch(luceneQuery).stream();
                           } catch (FetcherException e) {
                               LOGGER.warn(String.format("%s API request failed", searchBasedFetcher.getName()), e);
                               return Stream.empty();
                           }
                       })
                       .limit(maximumNumberOfReturnedResults)
                       .collect(Collectors.toList());
    }
}
