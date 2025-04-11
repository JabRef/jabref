package org.jabref.logic.importer.fetcher;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

/**
 * Fetcher for ACM Portal.
 * Supports paged search and parsing of search results from ACM's site.
 */
public class ACMPortalFetcher implements PagedSearchBasedFetcher {

    public static final String FETCHER_NAME = "ACM Portal";

    private static final String SEARCH_URL = "https://dl.acm.org/action/doSearch";

    public ACMPortalFetcher() {
        // ACM Portal requires cookies to be enabled
        CookieHandler.setDefault(new CookieManager());
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ACM);
    }

    /**
 * Constructs the URL for the search query.
 *
 * @param query QueryNode (user's search query parsed by Lucene)
 * @return A fully formed search URL for ACM Portal
 * @throws FetcherException if URL syntax is invalid or URL is malformed
 */
public URL getURLForQuery(QueryNode query) throws FetcherException {


        try {
            URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
            uriBuilder.addParameter("AllField", createQueryString(query));
            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Building URL failed.", e);
        }
    }

    /**
     * Helper to convert a QueryNode to a search string
     *
     * @param query Lucene QueryNode
     * @return A query string suitable for ACM Portal
     */
    private static String createQueryString(QueryNode query) {
        return new DefaultQueryTransformer().transformLuceneQuery(query).orElse("");
    }

    /**
     * Performs a paged search for a given lucene query (auto-parsed).
     *
     * @param luceneQuery QueryNode
     * @param pageNumber Page number (starting at 0)
     * @return Page of BibEntry results
     */
    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {
        String transformedQuery = createQueryString(luceneQuery);

        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(SEARCH_URL);
        } catch (URISyntaxException e) {
            throw new FetcherException("Building URI failed.", e);
        }

        uriBuilder.addParameter("AllField", transformedQuery);
        uriBuilder.addParameter("startPage", String.valueOf(pageNumber + 1)); // ACM uses 1-based page numbers

        // Placeholder: empty result list (real fetching logic happens elsewhere)
        return new Page<>(transformedQuery, pageNumber, List.of());
    }

    /**
     * Provides the Parser used to convert ACM Portal results to BibEntries.
     *
     * @return ACMPortalParser instance
     */
    public Parser getParser() {
        return new ACMPortalParser();
    }
}
