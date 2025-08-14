package org.jabref.logic.importer.fetcher;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.model.search.query.SearchQueryNode;

import org.apache.hc.core5.net.URIBuilder;

public class ACMPortalFetcher implements SearchBasedParserFetcher {

    public static final String FETCHER_NAME = "ACM Portal";

    private static final String SEARCH_URL = "https://dl.acm.org/action/doSearch";

    public ACMPortalFetcher() {
        // website dl.acm.org requires cookies
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

    private static String createQueryString(List<SearchQueryNode> queryList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (SearchQueryNode term : queryList) {
            stringBuilder.append(term.term());
            stringBuilder.append(" ");
        }
        return stringBuilder.toString().trim();
    }

    /**
     * Constructing the url for the searchpage.
     *
     * @param queryList list that contains the parsed nodes
     * @return query URL
     */
    @Override
    public URL getURLForQuery(List<SearchQueryNode> queryList) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("AllField", createQueryString(queryList));
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
