package org.jabref.model.study;

import org.jabref.model.database.BibDatabase;

/**
 * Represents the result of fetching the results for a query for a specific library
 */
public class FetchResult {
    private final String fetcherName;
    private final BibDatabase fetchResult;

    public FetchResult(String fetcherName, BibDatabase fetcherResult) {
        this.fetcherName = fetcherName;
        this.fetchResult = fetcherResult;
    }

    public String getFetcherName() {
        return fetcherName;
    }

    public BibDatabase getFetchResult() {
        return fetchResult;
    }
}
