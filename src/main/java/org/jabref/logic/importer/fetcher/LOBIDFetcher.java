package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.LOBIDQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

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
        uriBuilder.addParameter("q", new LOBIDQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // search query
        uriBuilder.addParameter("from", String.valueOf(getPageSize() * pageNumber)); // from entry number, starts indexing at 0
        uriBuilder.addParameter("size", String.valueOf(getPageSize())); // page size
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
        BibEntry entry = new BibEntry();
        Field nametype;

        // Publication type
        String isbn = getFirstArrayElement(jsonEntry, "isbn");
        if (StringUtil.isNullOrEmpty(isbn)) {
            // article
            entry.setType(StandardEntryType.Article);
            nametype = StandardField.JOURNAL;
        } else {
            // book chapter
            entry.setType(StandardEntryType.InCollection);
            nametype = StandardField.BOOKTITLE;
            entry.setField(StandardField.ISBN, isbn);
        }

        entry.setField(StandardField.ISSN, getFirstArrayElement(jsonEntry, "issn"));
        entry.setField(StandardField.TITLE, jsonEntry.optString("title", ""));

        // authors
        JSONArray authors = jsonEntry.optJSONArray("contribution");
        if (authors != null) {
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                JSONObject authorObject = authors.getJSONObject(i).optJSONObject("agent");
                String authorType = getFirstArrayElement(authorObject, "type");

                if (authorType.equals("Person")) {
                    authorList.add(authorObject.optString("label", ""));
                }
            }

            if (!authors.isEmpty()) {
                entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
            }
        } else {
            LOGGER.info("No author found.");
        }

        // publication
        Optional.ofNullable(jsonEntry.optJSONArray("publication"))
                .map(array -> array.getJSONObject(0))
                .ifPresent(publication -> {
                    entry.setField(nametype, getFirstArrayElement(publication, "publishedBy"));
                    String date = publication.optString("startDate");
                    entry.setField(StandardField.DATE, date);
                    entry.setField(StandardField.YEAR, date);
                });

        return entry;
    }

    private static String getFirstArrayElement(JSONObject jsonEntry, String key) {
        return Optional.ofNullable(jsonEntry.optJSONArray(key))
                       .map(array -> array.getString(0))
                       .orElse("");
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
