package org.jabref.gui.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.util.JSONEntryParser;
import org.jabref.logic.net.URLUtil;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetches data from the Directory of Open Access Journals (DOAJ)
 *
 * @implNote <a href="https://doaj.org/api/v1/docs">API documentation</a>
 */
public class DOAJFetcher implements SearchBasedParserFetcher {

    private static final String SEARCH_URL = "https://doaj.org/api/v1/search/articles/";
    private final ImportFormatPreferences preferences;

    public DOAJFetcher(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "DOAJ";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOAJ;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        URLUtil.addPath(uriBuilder, query);
        uriBuilder.addParameter("pageSize", "20"); // Number of results
        //uriBuilder.addParameter("page", "1"); // Page (not needed so far)
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONEntryParser jsonConverter = new JSONEntryParser();
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("results")) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject bibJsonEntry = results.getJSONObject(i).getJSONObject("bibjson");
                    BibEntry entry = jsonConverter.parseBibJSONtoBibtex(bibJsonEntry, preferences.getKeywordSeparator());
                    entries.add(entry);
                }
            }
            return entries;
        };
    }
}
