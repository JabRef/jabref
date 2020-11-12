package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.strings.StringUtil;

public interface PagedSearchBasedParserFetcher extends SearchBasedParserFetcher, PagedSearchBasedFetcher {

    @Override
    default Page<BibEntry> performSearchPaged(String query, int pageNumber) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return new Page<BibEntry>(query, pageNumber, Collections.emptyList());
        }

        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(query, pageNumber);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException(String.format("Search URI crafted from query %s is malformed", query), e);
        }
        return new Page<>(query, pageNumber, getBibEntries(urlForQuery));
    }

    private List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            throw new FetcherException("A network error occurred while fetching from " + urlForQuery, e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred while fetching from " + urlForQuery, e);
        }
    }

    @Override
    default Page<BibEntry> performComplexSearchPaged(ComplexSearchQuery complexSearchQuery, int pageNumber) throws FetcherException {
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getComplexQueryURL(complexSearchQuery, pageNumber);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        return new Page<>(complexSearchQuery.toString(), pageNumber, getBibEntries(urlForQuery));
    }

    /**
     * Constructs a URL based on the query, size and page number.
     *
     * @param query      the search query
     * @param pageNumber the number of the page indexed from 0
     */
    URL getURLForQuery(String query, int pageNumber) throws URISyntaxException, MalformedURLException;

    /**
     * Constructs a URL based on the query, size and page number.
     *
     * @param complexSearchQuery the search query
     * @param pageNumber         the number of the page indexed from 0
     */
    default URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery, int pageNumber) throws URISyntaxException, MalformedURLException {
        return getURLForQuery(complexSearchQuery.toString(), pageNumber);
    }

    @Override
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery) throws FetcherException {
        return SearchBasedParserFetcher.super.performComplexSearch(complexSearchQuery);
    }

    @Override
    default URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        return getURLForQuery(query, 0);
    }

    @Override
    default URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        return getComplexQueryURL(complexSearchQuery, 0);
    }
}
