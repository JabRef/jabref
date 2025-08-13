package org.jabref.logic.importer;

import java.util.List;

import org.jabref.logic.search.query.SearchQueryExtractorVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 * <p>
 *    This interface is used for web resources which directly return BibTeX data ({@link BibEntry})
 * </p>
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param queryList the list that contains the parsed nodes
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearch(List<SearchQueryNode> queryList) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param searchQuery query string that can be parsed into a search.g4 query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        if (searchQuery.isBlank()) {
            return List.of();
        }

        SearchQuery searchQueryObject = new SearchQuery(searchQuery);
        SearchQueryExtractorVisitor visitor = new SearchQueryExtractorVisitor(searchQueryObject.getSearchFlags());
        List<SearchQueryNode> queryList;
        try {
            queryList = visitor.visitStart(searchQueryObject.getContext());
        } catch (Exception e) {
            throw new FetcherException("An error occurred when parsing the query");
        }

        return this.performSearch(queryList);
    }
}
