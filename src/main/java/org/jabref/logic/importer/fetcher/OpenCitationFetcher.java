package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.CitationBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.google.common.collect.Lists;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to fetch for an articles' citation relations on opencitations.net's API
 */
public class OpenCitationFetcher implements CitationBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCitationFetcher.class);
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/metadata/";

    public OpenCitationFetcher() {
    }

    @Override
    public URL getURLForEntries(List<BibEntry> entries, SearchType searchType) throws URISyntaxException, MalformedURLException {
        String bibPart = entries.stream()
                                .map(entry -> entry.getField(StandardField.DOI))
                                .flatMap(Optional::stream)
                                .collect(Collectors.joining("__"));
        URIBuilder builder = new URIBuilder(BASIC_URL + bibPart);
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
                List<BibEntry> onlyDois = Arrays.stream(items)
                                                .map(doi -> new BibEntry().withField(StandardField.DOI, doi))
                                                .collect(Collectors.toList());
                try {
                    // prepare parallel download of citations
                    List<List<BibEntry>> partitions = Lists.partition(onlyDois, onlyDois.size() > 15 ? 10 : 2);
                    for (List<BibEntry> partList : partitions) {
                        URLDownload download = new URLDownload(getURLForEntries(partList, searchType));
                        JSONArray jsonArray = new JSONArray(download.asString(StandardCharsets.UTF_8));
                        if (jsonArray.isEmpty()) {
                            return entries;
                        }
                        for (int i = 0; i < jsonArray.length(); i++) {
                            entries.add(createNewEntry(jsonArray.getJSONObject(i)));
                        }
                    }
                } catch (IOException | URISyntaxException e) {
                    LOGGER.debug("Exception during fetching of citations", e);
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
                try {
                    String jsonText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    return new JSONArray(jsonText);
                } catch (IOException e) {
                    LOGGER.error("Error while converting input stream to JSONArray", e);
                    return new JSONArray();
                }
            }
        };
    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        LOGGER.atDebug()
              .addArgument(() -> "Articles citing " + entry.getField(StandardField.DOI).orElse("'No DOI found'"))
              .log("Search: {}");
        return performSearch(entry, SearchType.CITED_BY);
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        LOGGER.atDebug()
              .addArgument(() -> "Articles citing " + entry.getField(StandardField.DOI).orElse("'No DOI found'"))
              .log("Search: {}");
        return performSearch(entry, SearchType.CITING);
    }

    /**
     * Creates a new BibEntry from JSONObject
     *
     * @param jsonObject JSONObject
     * @return BibEntry created
     */
    private BibEntry createNewEntry(JSONObject jsonObject) {
        LOGGER.atDebug()
              .addArgument(() -> jsonObject.getString("doi"))
              .log("Paper found: {}");
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
