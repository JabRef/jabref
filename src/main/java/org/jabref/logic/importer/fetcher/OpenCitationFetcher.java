package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.jabref.logic.importer.CitationBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to fetch for an articles citation relations on opencitations.net's API
 */
public class OpenCitationFetcher implements CitationBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCitationFetcher.class);
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/metadata/";

    /**
     * Possible search methods
     */
    public enum SearchType {
        CITING("reference"),
        CITEDBY("citation"),
        BIBINFO("info");

        public final String label;

        SearchType(String label) {
            this.label = label;
        }
    }

    public OpenCitationFetcher() {

    }

    @Override
    public URL getURLForEntries(List<BibEntry> entries) throws URISyntaxException, MalformedURLException {
        StringJoiner stringJoiner = new StringJoiner("__");
        for (BibEntry entry : entries) {
            entry.getField(StandardField.DOI).ifPresent(stringJoiner::add);
        }
        URIBuilder builder = new URIBuilder(BASIC_URL + stringJoiner.toString());
        return builder.build().toURL();
    }

    @Override
    public Parser getParser(OpenCitationFetcher.SearchType searchType) {
        return inputStream -> {
            List<BibEntry> entries = new ArrayList<>();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            try {
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }
            } catch (IOException e) {
                return entries;
            }
            String jsonText = sb.toString();
            JSONArray json = new JSONArray(jsonText);
            if (json.isEmpty()) {
                return entries;
            }
            if (searchType.equals(SearchType.BIBINFO)) {
                for (int i = 0; i < json.length(); i++) {
                    entries.add(createNewEntry(json.getJSONObject(i)));
                }
            } else {
                String[] items = json.getJSONObject(0).getString(searchType.label).split("; ");
                if (items[0].isEmpty()) {
                    return entries;
                }
                for (String doi : items) {
                    entries.add(new BibEntry().withField(StandardField.DOI, doi));
                }
            }
            return entries;
        };
    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        LOGGER.debug("Search: {}", "Articles citing " + entry.getField(StandardField.DOI).orElse("'No DOI found'"));
        return performSearch(entry, SearchType.CITEDBY);
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        LOGGER.debug("Search: {}", "Articles cited by " + entry.getField(StandardField.DOI).orElse("'No DOI found'"));
        return performSearch(entry, SearchType.CITING);
    }

    /**
     * Creates a new BibEntry from JSONObject
     * @param jsonObject JSONObject
     * @return BibEntry created
     */
    private BibEntry createNewEntry(JSONObject jsonObject) {
        LOGGER.debug("Paper found: {}", jsonObject.getString("doi"));
        return new BibEntry()
                .withField(StandardField.TITLE, jsonObject.getString("title"))
                .withField(StandardField.AUTHOR, jsonObject.getString("author"))
                .withField(StandardField.YEAR, jsonObject.getString("year"))
                .withField(StandardField.PAGES, jsonObject.getString("page"))
                .withField(StandardField.VOLUME, jsonObject.getString("volume"))
                .withField(StandardField.ISSUE, jsonObject.getString("issue"))
                .withField(StandardField.DOI, jsonObject.getString("doi"));
    }

    @Override
    public String getName() {
        return "OpenCitationFetcher";
    }
}
