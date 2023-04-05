package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.MarcXmlParser;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class BvbFetcher implements SearchBasedParserFetcher {

    private static final String URL_PATTERN = "http://bvbr.bib-bvb.de:5661/bvb01sru?";

    @Override
    public String getName() {
        return "Bibliotheksverbund Bayern (Experimental)";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    @Override
    public URL getURLForQuery(QueryNode query) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("recordSchema", "marcxml");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", new DefaultQueryTransformer().transformLuceneQuery(query).orElse(""));
        uriBuilder.addParameter("maximumRecords", "30");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new MarcXmlParser();
    }
}
