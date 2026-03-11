package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.LoggerFactory;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /// @param queryNode  first search node
    /// @param pageNumber requested site number indexed from 0
    /// @return Page with search results
    Page<BibEntry> performSearchPaged(BaseQueryNode queryNode, int pageNumber) throws FetcherException;

    /// @param searchQuery query string that can be parsed into a lucene query
    /// @param pageNumber  requested site number indexed from 0
    /// @return Page with search results
    default Page<BibEntry> performSearchPaged(String searchQuery, int pageNumber) throws FetcherException {
        if (searchQuery.isBlank()) {
            return new Page<>(searchQuery, pageNumber, List.of());
        }

        SearchQuery searchQueryObject = new SearchQuery(searchQuery);
        BaseQueryNode queryNode;

        if (searchQueryObject.isValid()) {
            SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
            try {
                queryNode = visitor.visitStart(searchQueryObject.getContext());
            } catch (ParseCancellationException e) {
                LoggerFactory.getLogger(PagedSearchBasedFetcher.class).debug("Search query visitor failed for '{}', falling back to raw term search", searchQuery, e);
                queryNode = new SearchQueryNode(Optional.empty(), searchQuery);
            }
        } else {
            // Treat unparseable input as a raw unfielded term so fetchers pass it directly to their web API
            LoggerFactory.getLogger(PagedSearchBasedFetcher.class).debug("Search query '{}' is not valid ANTLR syntax, falling back to raw term search", searchQuery);
            queryNode = new SearchQueryNode(Optional.empty(), searchQuery);
        }

        return this.performSearchPaged(queryNode, pageNumber);
    }

    /// @return default pageSize
    default int getPageSize() {
        return 20;
    }

    /// This method is used to send complex queries using fielded search.
    ///
    /// @param queryNode the first search node
    /// @return a list of {@link BibEntry}, which are matched by the query (may be empty)
    @Override
    default List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        return new ArrayList<>(performSearchPaged(queryNode, 0).getContent());
    }
}
