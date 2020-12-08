package org.jabref.logic.importer.fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
import org.jabref.logic.importer.ParseException;
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

    public OpenCitationFetcher() {

    }

    @Override
    public URL getURLForEntries(List<BibEntry> entries, SearchType searchType) throws URISyntaxException, MalformedURLException {
        StringJoiner stringJoiner = new StringJoiner("__");
        for (BibEntry entry : entries) {
            entry.getField(StandardField.DOI).ifPresent(stringJoiner::add);
        }
        URIBuilder builder = new URIBuilder(BASIC_URL + stringJoiner.toString());
        return builder.build().toURL();
    }

    @Override
    public Parser getParser(SearchType searchType) {
        return new Parser() {

            @Override
            public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
                List<BibEntry> entries = new ArrayList<>();

                JSONArray json = parseJSONArray(inputStream);
                if (json.isEmpty()) {
                    return entries;
                }

                String[] items = json.getJSONObject(0).getString(searchType.label).split("; ");
                if (items[0].isEmpty()) {
                    return entries;
                }
                List<BibEntry> onlyDois = new ArrayList<>();
                for (String doi : items) {
                    onlyDois.add(new BibEntry().withField(StandardField.DOI, doi));
                }

                try (InputStream partInputStream = new BufferedInputStream(getURLForEntries(onlyDois, searchType).openStream())) {
                    JSONArray jsonArray = parseJSONArray(partInputStream);
                    if (jsonArray.isEmpty()) {
                        return entries;
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        entries.add(createNewEntry(jsonArray.getJSONObject(i)));
                    }
                } catch (IOException | URISyntaxException e) {
                    return entries;
                }
                return entries;
            }

            /**
             * Reads from InputStream and parses JSONArray
             * @param inputStream InputStream to use
             * @return JSONArray parsed
             */
            private JSONArray parseJSONArray(InputStream inputStream) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        stringBuilder.append((char) cp);
                    }
                    String jsonText = stringBuilder.toString();
                    return new JSONArray(jsonText);
                } catch (IOException e) {
                    return new JSONArray();
                }
            }
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
     *
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
