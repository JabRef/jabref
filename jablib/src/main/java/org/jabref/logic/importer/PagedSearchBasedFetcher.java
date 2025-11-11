package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param queryNode  first search node
     * @param pageNumber requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performSearchPaged(BaseQueryNode queryNode, int pageNumber) throws FetcherException;

    /**
     * @param searchQuery query string that can be parsed into a lucene query
     * @param pageNumber  requested site number indexed from 0
     * @return Page with search results
     */
    default Page<BibEntry> performSearchPaged(String searchQuery, int pageNumber) throws FetcherException {
        if (searchQuery.isBlank()) {
            return new Page<>(searchQuery, pageNumber, List.of());
        }
        SearchQuery searchQueryObject = new SearchQuery(searchQuery);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        try {
            return this.performSearchPaged(visitor.visitStart(searchQueryObject.getContext()), pageNumber);
        } catch (ParseCancellationException e) {
            throw new FetcherException("A syntax error occurred during parsing of the query");
        }
    }

    /**
     * @return default pageSize
     */
    default int getPageSize() {
        return 20;
    }

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param queryNode the first search node
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    @Override
    default List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        return new ArrayList<>(performSearchPaged(queryNode, 0).getContent());
    }
}
