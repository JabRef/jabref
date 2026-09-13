package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.DnbQueryTransformer;
import org.jabref.logic.importer.fileformat.MarcXmlParser;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class DnbFetcher extends AbstractIsbnFetcher implements PagedSearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnbFetcher.class);
    private static final String URL_PATTERN = "https://services.dnb.de/sru/dnb?";

    public DnbFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "DNB";
    }

    @Override
    public Parser getParser() {
        return new MarcXmlParser();
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException {
        String transformedQuery = new DnbQueryTransformer().transformSearchQuery(queryNode).orElse("").trim();
        if (transformedQuery.isEmpty()) {
            throw new URISyntaxException("", "Query transformed to an empty DNB search expression");
        }
        return buildSearchUrl(transformedQuery, pageNumber);
    }

    @Override
    // [impl->req~fetchers.dnb-search~1]
    public URL getURLForRawQuery(String rawQuery, int pageNumber) throws URISyntaxException, MalformedURLException {
        return buildSearchUrl(rawQuery, pageNumber);
    }

    @Override
    // [impl->req~fetchers.dnb-isbn-lookup~1]
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        this.ensureThatIsbnIsValid(identifier);
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", "num=" + identifier);
        uriBuilder.addParameter("recordSchema", "MARC21-xml");
        uriBuilder.addParameter("maximumRecords", "20");
        URL url = uriBuilder.build().toURL();
        LOGGER.debug("DNB URL: {}", url);
        return url;
    }

    private URL buildSearchUrl(String query, int pageNumber) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", query);
        uriBuilder.addParameter("recordSchema", "MARC21-xml");
        uriBuilder.addParameter("startRecord", String.valueOf(getPageSize() * pageNumber + 1));
        uriBuilder.addParameter("maximumRecords", String.valueOf(getPageSize()));
        return uriBuilder.build().toURL();
    }
}
