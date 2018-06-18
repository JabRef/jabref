package org.jabref.gui.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.util.JSONEntryParser;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetches data from the Springer
 *
 * @implNote see <a href="https://dev.springernature.com/">API documentation</a> for more details
 */
public class SpringerFetcher implements SearchBasedParserFetcher {

    private static final String API_URL = "http://api.springernature.com/meta/v1/json?q=";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";

    @Override
    public String getName() {
        return "Springer";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_SPRINGER;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("q", query); // Search query
        uriBuilder.addParameter("api_key", API_KEY); // API key
        uriBuilder.addParameter("p", "20"); // Number of results to return
        //uriBuilder.addParameter("s", "1"); // Start item (not needed at the moment)
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("records")) {
                JSONArray results = jsonObject.getJSONArray("records");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jsonEntry = results.getJSONObject(i);
                    BibEntry entry = JSONEntryParser.parseSpringerJSONtoBibtex(jsonEntry);
                    entries.add(entry);
                }
            }

            return entries;
        };
    }
}
