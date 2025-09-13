package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;

public interface PagedSearchBasedParserFetcher extends SearchBasedParserFetcher, PagedSearchBasedFetcher, ParserFetcher {

    @Override
    default Page<BibEntry> performSearchPaged(BaseQueryNode queryNode, int pageNumber) throws FetcherException {
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(queryNode, pageNumber);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        return new Page<>(queryNode.toString(), pageNumber, getBibEntries(urlForQuery));
    }

    private List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            throw new FetcherException(urlForQuery, "A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForQuery, "An internal parser error occurred", e);
        }
    }

    /**
     * Constructs a URL based on the query, size and page number.
     *
     * @param queryNode  the first search node
     * @param pageNumber the number of the page indexed from 0
     */
    URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException;

    @Override
    default URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        return getURLForQuery(queryNode, 0);
    }

    @Override
    default List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        return SearchBasedParserFetcher.super.performSearch(queryNode);
    }
}
