package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.MarcXmlParser;

import org.apache.http.client.utils.URIBuilder;

public class BvbFetcher implements SearchBasedParserFetcher {

    private static final String URL_PATTERN = "http://bvbr.bib-bvb.de:5661/bvb01sru?";

    @Override
    public String getName() {
        return "BVB";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    protected String getSearchQueryString(String query) {
        Objects.requireNonNull(query);
        return query;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException {
        String gvkQuery = getSearchQueryString(query);
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("recordSchema", "marcxml");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", gvkQuery);
        uriBuilder.addParameter("maximumRecords", "10");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new MarcXmlParser();
    }
}
