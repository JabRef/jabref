package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompositeSearchBasedFetcher implements SearchBasedFetcher {
    public static volatile Boolean onPerformSucceed = false;

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

    /**
     * CS304 Issue Link: https://github.com/JabRef/jabref/issues/7606
     * Set onPerformSucceed to true at performSearchPaged method in ArXiv
     */
    public static void PerformSucceed(){
        onPerformSucceed = true;
    }
    // keep a status of perform search to make sure the execute order
    @Override
    public String getName() {
        return "SearchAll";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    /**
     * CS304 Issue Link: https://github.com/JabRef/jabref/issues/7606
     *
     * @param luceneQuery the root node of the lucene query
     *
     * @return return the fetchers stream
     *
     * @throws FetcherException when API request failed at searchBasedFetcher.getName()
     */
    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        ImportCleanup cleanup = new ImportCleanup(BibDatabaseMode.BIBTEX);
        // All entries have to be converted into one format, this is necessary for the format conversion
        return fetchers.stream()
                       .flatMap(searchBasedFetcher -> {
                           try {
                               onPerformSucceed  = false;
                               return searchBasedFetcher.performSearch(luceneQuery).stream();
                           } catch (FetcherException e) {
                               LOGGER.warn(String.format("%s API request failed", searchBasedFetcher.getName()), e);
                               return Stream.empty();
                           }
                       })
                       .limit(maximumNumberOfReturnedResults)
                       .map(x->{
                           while (!onPerformSucceed) {
                               Thread.onSpinWait();
                           }
                           return cleanup.doPostCleanup(x);
                            })
                       .collect(Collectors.toList());
    }
}
//clear only after the perform search
