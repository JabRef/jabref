package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.CitationFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to fetch for an articles citation relations on opencitations.net's API
 */
public class OpenCitationFetcher implements CitationFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCitationFetcher.class);
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/metadata/";

    /**
     * Possible search methods
     */
    public enum SearchType {
        CITING("reference"),
        CITEDBY("citation");

        public final String label;

        SearchType(String label) {
            this.label = label;
        }
    }

    public OpenCitationFetcher() {

    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        return performSearch(entry, SearchType.CITEDBY);
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        return performSearch(entry, SearchType.CITING);
    }

    /**
     * Executes the search method associated with searchType
     *
     * @param entry Entry to search relations for
     * @return List of BibEntries found
     */
    public List<BibEntry> performSearch(BibEntry entry, SearchType searchType) throws FetcherException {
        String doi = entry.getField(StandardField.DOI).orElse("");
        if (searchType != null) {
            List<BibEntry> list = new ArrayList<>();
            try {
                LOGGER.debug("Search: {}", BASIC_URL + doi);
                JSONArray json = readJsonFromUrl(BASIC_URL + doi);
                if (json == null) {
                    throw new FetcherException("No internet connection! Please try again.");
                } else if (json.isEmpty()) {
                    return list;
                }
                LOGGER.debug("API Answer: " + json.toString());
                String[] items = json.getJSONObject(0).getString(searchType.label).split("; ");
                if (!Arrays.equals(items, new String[]{""})) {
                    LOGGER.debug("BibInfoSearch: {}", BASIC_URL + String.join("__", items));
                    JSONArray metaArray = readJsonFromUrl(BASIC_URL + String.join("__", items));
                    if (metaArray == null) {
                        throw new FetcherException("No internet connection! Please try again.");
                    } else if (metaArray.isEmpty()) {
                        return list;
                    }
                    LOGGER.debug("API Answer: " + metaArray.toString());
                    for (int i = 0; i < metaArray.length(); i++) {
                        BibEntry newEntry = new BibEntry();
                        newEntry.setField(StandardField.TITLE, metaArray.getJSONObject(i).getString("title"));
                        newEntry.setField(StandardField.AUTHOR, metaArray.getJSONObject(i).getString("author"));
                        newEntry.setField(StandardField.YEAR, metaArray.getJSONObject(i).getString("year"));
                        newEntry.setField(StandardField.PAGES, metaArray.getJSONObject(i).getString("page"));
                        newEntry.setField(StandardField.VOLUME, metaArray.getJSONObject(i).getString("volume"));
                        newEntry.setField(StandardField.ISSUE, metaArray.getJSONObject(i).getString("issue"));
                        newEntry.setField(StandardField.DOI, metaArray.getJSONObject(i).getString("doi"));
                        list.add(newEntry);
                    }
                }
            } catch (IOException | JSONException e) {
                throw new FetcherException("Couldn't connect to opencitations.net! Please try again." + e);
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

    public String getName() {
        return "CitationRelationFetcher";
    }
}
