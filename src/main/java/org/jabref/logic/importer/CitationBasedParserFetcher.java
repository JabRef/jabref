package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.fetcher.OpenCitationFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;

/**
 * Provides a convenient interface for citation-based fetcher, which follow the usual four-step procedure:
 * 1. Send request with doi of the entry
 * 2. Parse the response to get a list of DOI's
 * 3. Send new request to get bibliographic information for every DOI
 * 4. Parse the response and create a List of {@link BibEntry}
 */
public interface CitationBasedParserFetcher extends CitationFetcher {

    /**
     * Constructs a URL-String based on the given DOI's.
     *
     * @param dois the dois to look information for
     */
    String getURLForEntries(String... dois);

    default List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);
        return performSearch(entry, OpenCitationFetcher.SearchType.CITEDBY);
    }

    default List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);
        return performSearch(entry, OpenCitationFetcher.SearchType.CITING);
    }

    /**
     * Searches specified URL for related articles based on a {@link BibEntry}
     * and the citation type {@link org.jabref.logic.importer.fetcher.OpenCitationFetcher.SearchType}
     *
     * @param entry      entry to search bibliographic information for
     * @param searchType type of search to perform (CITING, CITEDBY)
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     * @throws FetcherException Error message passed to {@link org.jabref.gui.entryeditor.CitationRelationsTab}
     */
    default List<BibEntry> performSearch(BibEntry entry, OpenCitationFetcher.SearchType searchType) throws FetcherException {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(searchType);
        String doi = entry.getField(StandardField.DOI).orElse("");

        List<BibEntry> list = new ArrayList<>();
        try {
            JSONArray json = Objects.requireNonNull(readJsonFromUrl(getURLForEntries(doi)));
            if (json.isEmpty()) {
                return list;
            }
            String[] items = json.getJSONObject(0).getString(searchType.label).split("; ");
            if (!Arrays.equals(items, new String[] {""})) {
                JSONArray metaArray = Objects.requireNonNull(readJsonFromUrl(getURLForEntries(items)));
                if (metaArray.isEmpty()) {
                    return list;
                }
                for (int i = 0; i < metaArray.length(); i++) {
                    list.add(createNewEntry(metaArray.getJSONObject(i)));
                }
            }
        } catch (IOException | JSONException | NullPointerException e) {
            throw new FetcherException("Couldn't connect to API! Please try again.");
        }
        return list;
    }

    default BibEntry createNewEntry(JSONObject jsonObject) {
        // Create new BibEntry from API response
        return null;
    }

    /**
     * Method to read JSON files from URL-String
     *
     * @param url API URL to search
     * @return JSONArray containing the response of the API
     */
    default JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            return new JSONArray(jsonText);
        } catch (UnknownHostException | SocketException exception) {
            return null;
        }
    }
}
