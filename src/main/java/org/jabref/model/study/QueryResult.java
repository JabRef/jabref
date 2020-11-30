package org.jabref.model.study;

import java.util.List;

/**
 * Represents the result of fetching the results from all active fetchers for a specific query.
 */
public class QueryResult {
    private final String query;
    private final List<FetchResult> resultsPerLibrary;

    public QueryResult(String query, List<FetchResult> resultsPerLibrary) {
        this.query = query;
        this.resultsPerLibrary = resultsPerLibrary;
    }

    public String getQuery() {
        return query;
    }

    public List<FetchResult> getResultsPerFetcher() {
        return resultsPerLibrary;
    }
}
