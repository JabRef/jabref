package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.search.query.SearchQueryExtractorVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param queryList the list that contains the parsed nodes
     * @param pageNumber       requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performSearchPaged(List<SearchQueryNode> queryList, int pageNumber) throws FetcherException;

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
        SearchQueryExtractorVisitor visitor = new SearchQueryExtractorVisitor(searchQueryObject.getSearchFlags());
        try {
            return this.performSearchPaged(visitor.visitStart(searchQueryObject.getContext()), pageNumber);
        } catch (Exception e) {
            throw new FetcherException("An error occurred during parsing of the query.");
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
     * @param queryList the list that contains the parsed nodes
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    @Override
    default List<BibEntry> performSearch(List<SearchQueryNode> queryList) throws FetcherException {
        return new ArrayList<>(performSearchPaged(queryList, 0).getContent());
    }
}
