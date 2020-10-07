package org.jabref.logic.importer.fetcher;

import org.apache.http.client.utils.URIBuilder;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.JstorParser;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Fetcher for Jstor
 **/
public class JstorFetcher implements SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JstorFetcher.class);
    private static final String HOST = "https://www.jstor.org/open/search/";

    private final ImportFormatPreferences importFormatPreferences;

    public JstorFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        LOGGER.warn(query);
        URIBuilder uriBuilder = new URIBuilder(HOST);
        uriBuilder.addParameter("Query", query); // Query
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new JstorParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Override
    public String getName() {
        return "JSTOR";
    }
}
