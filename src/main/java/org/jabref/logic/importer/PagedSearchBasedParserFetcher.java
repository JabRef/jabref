package org.jabref.logic.importer;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public interface PagedSearchBasedParserFetcher extends SearchBasedParserFetcher, PagedSearchBasedFetcher {

    /**
     * Constructs a URL based on the query, size and page number.
     * @param query the search query
     * @param size the size of the page
     * @param pageNumber the number of the page
     * */
    URL getURLForQuery(String query, int size, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException;
}
