package org.jabref.logic.importer.fetcher;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

/**
 * Class to fetch for an articles citation relations on opencitations.net's API
 */
public class CitationRelationFetcher implements EntryBasedFetcher {

    private SearchType searchType = null;
    private Label progressLabel;
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/metadata/";

    /**
     * Possible search methods
     */
    public enum SearchType {
        CITING("reference"),
        CITEDBY("citation");

        public final String label;

        private SearchType(String label) {
            this.label = label;
        }
    }

    public CitationRelationFetcher(SearchType searchType, Label progressLabel) {
        this.searchType = searchType;
        this.progressLabel = progressLabel;
    }

    /**
     * Executes the search method associated with searchType
     *
     * @param entry Entry to search relations for
     * @return List of BibEntries found
     */
    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        String doi = entry.getField(StandardField.DOI).orElse("");
        if (searchType != null) {
            List<BibEntry> list = new ArrayList<>();
            try {
                System.out.println(BASIC_URL + doi);
                JSONArray json = readJsonFromUrl(BASIC_URL + doi);
                if (json == null) {
                    throw new FetcherException("No internet connection! Please try again.");
                } else if (json.isEmpty()) {
                    return null;
                }
                System.out.println(json.toString());
                String[] items = json.getJSONObject(0).getString(searchType.label).split("; ");
                int i = 1;
                for (String item : items) {
                    int finalI = i;
                    Platform.runLater(() -> progressLabel.setText(finalI + "/" + items.length));
                    System.out.println(item);
                    if (!doi.equals(item) && !item.equals("")) {
                        DoiFetcher doiFetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
                        try {
                            doiFetcher.performSearchById(item).ifPresent(list::add);
                        } catch (FetcherException fetcherException) {
                            // No information for doi found
                        }
                    }
                    i++;
                }
                System.out.println("Finished.");
            } catch (IOException | JSONException e) {
                throw new FetcherException("Couldn't connect to opencitations.net! Please try again.");
            }
            return list;
        } else {
            return null;
        }
    }

    /**
     * Method to read JSON files from URL
     *
     * @param url API URL to search
     * @return JSONArray containing the response of the API
     */
    public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        } catch (UnknownHostException | SocketException exception) {
            return null;
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return null;
    }
}
