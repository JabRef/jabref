package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;

/**
 * A class for fetching DOIs from Medra
 *
 * @see <a href="https://data.medra.org">mEDRA Content Negotiation API</a> for an overview of the API
 * <p>
 * It requires "Accept" request Header attribute to be set to desired content-type.
 */
public class Medra implements SearchBasedParserFetcher {

    public static final String API_URL = "https://data.medra.org";
    public static final String CONTENT_TYPE_JSON = "application/vnd.citationstyles.csl+json";

    @Override
    public String getName() {
        return "mEDRA";
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);

            List<BibEntry> entries = new ArrayList<>();
            BibEntry entry = jsonItemToBibEntry(response);
            entries.add(entry);

            return entries;
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry();
            entry.setType(convertType(item.getString("type")));
            entry.setField(StandardField.TITLE, item.getString("title"));
            entry.setField(StandardField.SUBTITLE,
                           Optional.ofNullable(item.optJSONArray("subtitle"))
                                   .map(array -> array.optString(0)).orElse(""));
            entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("author")));
            entry.setField(StandardField.YEAR,
                           Optional.ofNullable(item.optJSONObject("issued"))
                                   .map(array -> array.optJSONArray("date-parts"))
                                   .map(array -> array.optJSONArray(0))
                                   .map(array -> array.optInt(0))
                                   .map(year -> Integer.toString(year)).orElse(""));
            entry.setField(StandardField.DOI, item.getString("DOI"));
            entry.setField(StandardField.PAGES, item.optString("page"));
            entry.setField(StandardField.ISSN, item.optString("ISSN"));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("mEdRA API JSON format has changed", exception);
        }
    }

    private EntryType convertType(String type) {
        switch (type) {
            case "article-journal":
                return StandardEntryType.Article;
            default:
                return StandardEntryType.Misc;
        }
    }

    private String toAuthors(JSONArray authors) {
        if (authors == null) {
            return "";
        }

        // input: list of {"literal":"A."}
        AuthorList authorsParsed = new AuthorList();
        for (int i = 0; i < authors.length(); i++) {
            JSONObject author = authors.getJSONObject(i);
            authorsParsed.addAuthor(
                                    author.optString("literal", ""),
                                    "",
                                    "",
                                    "",
                                    "");
        }
        return authorsParsed.getAsFirstLastNamesWithAnd();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        SearchBasedParserFetcher.super.doPostCleanup(entry);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/" + query);
        return uriBuilder.build().toURL();
    }

}
