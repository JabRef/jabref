package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.CollectionOfComputerScienceBibliographiesParser;

import com.microsoft.applicationinsights.core.dependencies.http.client.utils.URIBuilder;

public class CollectionOfComputerScienceBibliographiesFetcher implements SearchBasedParserFetcher {
    private static final String BASIC_SEARCH_URL = "http://liinwww.ira.uka.de/bibliography/rss?";

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
        uriBuilder.addParameter("query", query);
        uriBuilder.addParameter("sort", "score");
        URI uri = uriBuilder.build();
        return uri.toURL();
    }

    @Override
    public Parser getParser() {
        return new CollectionOfComputerScienceBibliographiesParser();
    }

    @Override
    public String getName() {
        return "Collection of Computer Science Bibliographies";
    }
}
