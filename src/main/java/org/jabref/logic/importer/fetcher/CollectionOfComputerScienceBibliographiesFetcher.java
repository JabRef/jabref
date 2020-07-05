package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;

import org.apache.http.client.utils.URIBuilder;

public class CollectionOfComputerScienceBibliographiesFetcher implements SearchBasedParserFetcher {

    private static final String BASIC_SEARCH_URL = "http://liinwww.ira.uka.de/bibliography/rss?";

    private final CollectionOfComputerScienceBibliographiesParser parser;

    public CollectionOfComputerScienceBibliographiesFetcher(ImportFormatPreferences importFormatPreferences) {
        this.parser = new CollectionOfComputerScienceBibliographiesParser(importFormatPreferences);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        return new URIBuilder(BASIC_SEARCH_URL)
            .addParameter("query", query)
            .addParameter("sort", "score")
            .build()
            .toURL();
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public String getName() {
        return "Collection of Computer Science Bibliographies";
    }
}
