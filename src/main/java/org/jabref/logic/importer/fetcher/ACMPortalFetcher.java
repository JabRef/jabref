package org.jabref.logic.importer.fetcher;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.ACMPortalParser;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class ACMPortalFetcher implements SearchBasedParserFetcher {

    private static final String SEARCH_URL = "https://dl.acm.org/action/doSearch";

    public ACMPortalFetcher() {
        // website dl.acm.org requires cookies
        CookieHandler.setDefault(new CookieManager());
    }

    @Override
    public String getName() {
        return "ACM Portal";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ACM);
    }

    private static String createQueryString(QueryNode query) {
        return new DefaultQueryTransformer().transformLuceneQuery(query).orElse("");
    }

    /**
     * Constructing the url for the searchpage.
     *
     * @param query query node
     * @return query URL
     */
    @Override
    public URL getURLForQuery(QueryNode query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("AllField", createQueryString(query));
        return uriBuilder.build().toURL();
    }

    /**
     * Gets an instance of ACMPortalParser.
     *
     * @return the parser which can process the results returned from the ACM Portal search page
     */
    @Override
    public Parser getParser() {
        return new ACMPortalParser();
    }

}
