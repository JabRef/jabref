package org.jabref.logic.importer;

import java.util.List;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 * <p>
 * This interface is used for web resources which directly return BibTeX data ({@link BibEntry})
 * </p>
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param queryList the list that contains the parsed nodes
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param searchQuery query string that can be parsed into a search query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        if (searchQuery.isBlank()) {
            return List.of();
        }

        SearchQuery searchQueryObject = new SearchQuery(searchQuery);
        if (!searchQueryObject.isValid()) {
            throw new FetcherException("The query is not valid");
        }
        BaseQueryNode queryNode;
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        try {
            queryNode = visitor.visitStart(searchQueryObject.getContext());
        } catch (ParseCancellationException e) {
            throw new FetcherException("A syntax error occurred during parsing of the query");
        }

        return this.performSearch(queryNode);
    }
}
