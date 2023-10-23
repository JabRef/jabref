package org.jabref.logic.importer.fetcher;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.ScholarArchiveQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScholarArchiveFetcher implements PagedSearchBasedParserFetcher {

    // Define a constant for the fetcher name.
    public static final String FETCHER_NAME = "ScholarArchive";

    // Initialize the logger for this class.
    private static final Logger LOGGER = LoggerFactory.getLogger(ScholarArchiveFetcher.class);

    // Define the API URL for ScholarArchive.
    private static final String API_URL = "https://scholar.archive.org/search";

    @Override
    /**
     * Gets the query URL.
     *
     * @param luceneQuery the search query
     * @param pageNumber  the number of the page indexed from 0
     * @return URL
     */
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);

        // Add search query parameter to the URL.
        uriBuilder.addParameter("q", new ScholarArchiveQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));

        // Add page number and page size parameters to the URL.
        uriBuilder.addParameter("from", String.valueOf(getPageSize() * pageNumber));
        uriBuilder.addParameter("size", String.valueOf(getPageSize()));

        // Specify the response format as JSON.
        uriBuilder.addParameter("format", "json");

        // Build the URL.
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            // Read the API response into a string.
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));

            // Parse the JSON response into a list of BibEntry objects.
            JSONObject jsonObject = new JSONObject(response);
            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("results")) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jsonEntry = results.getJSONObject(i);
                    BibEntry entry = parseJSONtoBibtex(jsonEntry);
                    entries.add(entry);
                }
            }

            return entries;
        };
    }


    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    private BibEntry parseJSONtoBibtex(JSONObject jsonEntry) throws ParseException {
        try{
        BibEntry entry = new BibEntry();
        EntryType entryType = StandardEntryType.InCollection;
        JSONObject biblio = jsonEntry.optJSONObject("biblio");
        JSONObject abstracts = jsonEntry.optJSONObject("abstracts");

            // publication type
            String type = biblio.optString("release_type");
            entry.setField(StandardField.TYPE, type);
            if (type.toLowerCase().contains("book")) {
                entryType = StandardEntryType.Book;
            } else if (type.toLowerCase().contains("article")) {
                entryType = StandardEntryType.Article;
            }
            entry.setType(entryType);


            entry.setField(StandardField.TITLE, biblio.optString("title"));
            entry.setField(StandardField.JOURNAL,biblio.optString("container_name"));
            entry.setField(StandardField.DOI,biblio.optString("doi"));
            entry.setField(StandardField.ISSUE,biblio.optString("issue"));
            entry.setField(StandardField.LANGUAGE,biblio.optString("lang_code"));
            entry.setField(StandardField.PUBLISHER,biblio.optString("publisher"));

            entry.setField(StandardField.YEAR, String.valueOf(biblio.optInt("release_year")));
            entry.setField(StandardField.VOLUME,String.valueOf(biblio.optInt("volume_int")));
            entry.setField(StandardField.ABSTRACT,abstracts.optString("body"));

            // Date
            String dateString = (String) biblio.get("date");

            entry.setField(StandardField.DATE,dateString);

            // Authors
            if (biblio.has("contrib_names")) {
                JSONArray authors = jsonEntry.getJSONArray("contrib_names");
                List<String> authorList = new ArrayList<>();
                if(authors!=null && authors.length()>0){
                    for (int i = 0; i < authors.length(); i++) {
                        authorList.add(authors.getString(i));
                    }
                }
                entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
            } else {
                LOGGER.info("No author found.");
            }

            // ISSN
            if (biblio.has("issns")) {
                JSONArray Issns = jsonEntry.getJSONArray("issns");
                List<String> IssnsList = new ArrayList<>();
                if(Issns!=null && Issns.length()>0){
                    for (int i = 0; i < Issns.length(); i++) {
                        IssnsList.add(Issns.getString(i));
                    }
                }
                entry.setField(StandardField.ISSN, String.join(" and ", IssnsList));
            } else {
                LOGGER.info("No ISSN found.");
            }

            return entry;
        }
        catch (JSONException exception) {
            throw new ParseException("ScholarArchive API JSON format has changed", exception);
        }

    }

}
