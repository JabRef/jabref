package org.jabref.logic.importer;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Searches web resources for bibliographic information based on a free-text query.
/// May return multiple search hits.
///
/// This interface is used for web resources which directly return BibTeX data ({@link BibEntry})
///
@NullMarked
public interface SearchBasedFetcher extends WebFetcher {
    /// This method is used to send complex queries using fielded search.
    ///
    /// @param queryList the list that contains the parsed nodes
    /// @return a list of {@link BibEntry}, which are matched by the query (may be empty)
    List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException;

    /// Looks for hits which are matched by the given free-text query.
    ///
    /// @param searchQuery query string that can be parsed into a search query
    /// @return a list of {@link BibEntry}, which are matched by the query (may be empty)
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        if (searchQuery.isBlank()) {
            return List.of();
        }

        return this.performSearch(getQueryNode(searchQuery));
    }

    /// This method provides a BaseQueryNode for performSearch/performSearchPaged method
    static BaseQueryNode getQueryNode(String searchQuery) {
        // Interface does not allow private constants
        final Logger LOGGER = LoggerFactory.getLogger(SearchBasedFetcher.class);

        SearchQuery searchQueryObject = new SearchQuery(searchQuery);
        BaseQueryNode queryNode;

        if (searchQueryObject.isValid()) {
            SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
            try {
                queryNode = visitor.visitStart(searchQueryObject.getContext());
            } catch (ParseCancellationException e) {
                LOGGER.debug("Search query visitor failed for '{}', falling back to raw term search", searchQuery, e);
                queryNode = new SearchQueryNode(Optional.empty(), searchQuery);
            }
        } else {
            // Treat unparseable input as a raw unfielded term so fetchers pass it directly to their web API
            LOGGER.debug("Search query '{}' is not valid ANTLR syntax, falling back to raw term search", searchQuery);
            queryNode = new SearchQueryNode(Optional.empty(), searchQuery);
        }
        return queryNode;
    }
}
