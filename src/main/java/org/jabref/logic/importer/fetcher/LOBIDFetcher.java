package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.LOBIDQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the LOBID API
 *
 * @see <a href="https://lobid.org/resources/api">API documentation</a> for more details
 */
public class LOBIDFetcher implements PagedSearchBasedParserFetcher {

    public static final String FETCHER_NAME = "LOBID";

    private static final Logger LOGGER = LoggerFactory.getLogger(LOBIDFetcher.class);

    private static final String API_URL = "https://lobid.org/resources/search";

    private final ImporterPreferences importerPreferences;

    public LOBIDFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /**
     * Gets the query URL
     *
     * @param luceneQuery the search query
     * @param pageNumber  the number of the page indexed from 0
     * @return URL
     */
    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {

        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("q", new LOBIDQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // Search query
        uriBuilder.addParameter("from", String.valueOf(getPageSize() * pageNumber)); // From entry number, starts indexing at 0
        uriBuilder.addParameter("size", String.valueOf(getPageSize())); // Page size
        uriBuilder.addParameter("format", "json"); // response format
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("member")) {
                JSONArray results = jsonObject.getJSONArray("member");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jsonEntry = results.getJSONObject(i);
                    BibEntry entry = parseJSONtoBibtex(jsonEntry);
                    entries.add(entry);
                }
            }

            return entries;
        };
    }

    private BibEntry parseJSONtoBibtex(JSONObject jsonEntry) {
        return null;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
