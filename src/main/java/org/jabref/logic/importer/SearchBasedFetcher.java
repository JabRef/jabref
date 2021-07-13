package org.jabref.logic.importer;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param luceneQuery the root node of the lucene query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param searchQuery query string that can be parsed into a lucene query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        if (searchQuery.isBlank()) {
            return Collections.emptyList();
        }

        SyntaxParser parser = new StandardSyntaxParser();
        QueryNode queryNode;
        try {
            queryNode = parser.parse(searchQuery, NO_EXPLICIT_FIELD);
        } catch (QueryNodeParseException e) {
            throw new FetcherException("An error occurred when parsing the query");
        }

        return this.performSearch(queryNode);
    }
}
