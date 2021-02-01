package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param luceneQuery the root node of the lucene query
     * @param pageNumber       requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException;

    /**
     * @param searchQuery query string that can be parsed into a lucene query
     * @param pageNumber  requested site number indexed from 0
     * @return Page with search results
     */
    default Page<BibEntry> performSearchPaged(String searchQuery, int pageNumber) throws FetcherException {
        if (searchQuery.isBlank()) {
            return new Page<>(searchQuery, pageNumber, Collections.emptyList());
        }
        SyntaxParser parser = new StandardSyntaxParser();
        final String NO_EXPLICIT_FIELD = "default";
        try {
            return this.performSearchPaged(parser.parse(searchQuery, NO_EXPLICIT_FIELD), pageNumber);
        } catch (QueryNodeParseException e) {
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
     * @param luceneQuery the root node of the lucene query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        return new ArrayList<>(performSearchPaged(luceneQuery, 0).getContent());
    }

}
