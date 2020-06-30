package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.net.URLDownload;
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
public class Medra implements SearchBasedParserFetcher, IdBasedParserFetcher {

    public static final String API_URL = "https://data.medra.org";

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
            entry.setField(StandardField.JOURNAL, item.optString("container-title"));
            entry.setField(StandardField.PUBLISHER, item.optString("publisher"));
            entry.setField(StandardField.URL, item.optString("URL"));
            entry.setField(StandardField.VOLUME, item.optString("volume"));
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
        String name = "";

        for (int i = 0; i < authors.length(); i++) {
            JSONObject author = authors.getJSONObject(i);
            if (author.has("literal")) {
                name = author.optString("literal", "");
            } else {
                name = author.optString("family", "") + " " + author.optString("given", "");
            }

            authorsParsed.addAuthor(
                                    name,
                                    "",
                                    "",
                                    "",
                                    "");

        }
        return authorsParsed.getAsFirstLastNamesWithAnd();
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {

        try (InputStream stream = getUrlDownload(identifier).asInputStream();
             PushbackInputStream pushbackInputStream = new PushbackInputStream(stream)) {

            List<BibEntry> fetchedEntries = new ArrayList<>();

            // check if there is anything to read since mEDRA '404 not found' returns nothing
            int readByte;
            readByte = pushbackInputStream.read();
            if (readByte != -1) {
                pushbackInputStream.unread(readByte);
                fetchedEntries = getParser().parseEntries(pushbackInputStream);
            }

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }

            BibEntry entry = fetchedEntries.get(0);

            // Post-cleanup
            doPostCleanup(entry);

            return Optional.of(entry);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401 errors and report that user has no rights to access resource. It might be that there is an UnknownHostException (eutils.ncbi.nlm.nih.gov cannot be resolved).
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        IdBasedParserFetcher.super.doPostCleanup(entry);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/" + query);
        return uriBuilder.build().toURL();
    }

    @Override
    public URLDownload getUrlDownload(String identifier) throws MalformedURLException, FetcherException, URISyntaxException {
        URLDownload download = new URLDownload(getURLForID(identifier));
        download.addHeader("Accept", MediaTypes.APPLICATION_JSON);
        return download;
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/" + identifier);
        return uriBuilder.build().toURL();
    }

}
