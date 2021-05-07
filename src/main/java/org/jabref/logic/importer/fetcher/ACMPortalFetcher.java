package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class ACMPortalFetcher implements SearchBasedParserFetcher {

    private static final String SEARCH_URL = "https://dl.acm.org/exportformats_search.cfm";

    private final ImportFormatPreferences preferences;

    public ACMPortalFetcher(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "ACM Portal";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ACM);
    }

    private static String createQueryString(QueryNode query) throws FetcherException {
        String queryString = new DefaultQueryTransformer().transformLuceneQuery(query).orElse("");
        // Query syntax to search for an entry that matches "one" and "two" in any field is: (+one +two)
        return "(%252B" + queryString.trim().replaceAll("\\s+", "%20%252B") + ")";
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("query", createQueryString(luceneQuery)); // Search all fields
        uriBuilder.addParameter("within", "owners.owner=GUIDE"); // Search within the ACM Guide to Computing Literature (encompasses the ACM Full-Text Collection)
        uriBuilder.addParameter("expformat", "bibtex"); // BibTeX format
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences, new DummyFileUpdateMonitor());
    }
}
