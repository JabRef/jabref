package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.IdParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.OptionalUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;

/**
 * A class for fetching DOIs from CrossRef
 * <p>
 * See https://github.com/CrossRef/rest-api-doc
 */
public class CrossRef implements IdParserFetcher<DOI>, EntryBasedParserFetcher, SearchBasedParserFetcher, IdBasedParserFetcher {

    private static final String API_URL = "https://api.crossref.org/works";

    private static final RemoveBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveBracesFormatter();

    @Override
    public String getName() {
        return "Crossref";
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        entry.getLatexFreeField(StandardField.TITLE).ifPresent(title -> uriBuilder.addParameter("query.bibliographic", title));
        entry.getLatexFreeField(StandardField.AUTHOR).ifPresent(author -> uriBuilder.addParameter("query.author", author));
        entry.getLatexFreeField(StandardField.YEAR).ifPresent(year ->
                uriBuilder.addParameter("filter", "from-pub-date:" + year)
        );
        uriBuilder.addParameter("rows", "20"); // = API default
        uriBuilder.addParameter("offset", "0"); // start at beginning
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("query", query);
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/" + identifier);
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream).getJSONObject("message");

            List<BibEntry> entries = new ArrayList<>();
            if (response.has("items")) {
                // Response contains a list
                JSONArray items = response.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    BibEntry entry = jsonItemToBibEntry(item);
                    entries.add(entry);
                }
            } else {
                // Singleton response
                BibEntry entry = jsonItemToBibEntry(response);
                entries.add(entry);
            }

            return entries;
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Sometimes the fetched entry returns the title also in the subtitle field; in this case only keep the title field
        if (entry.getField(StandardField.TITLE).equals(entry.getField(StandardField.SUBTITLE))) {
            new FieldFormatterCleanup(StandardField.SUBTITLE, new ClearFormatter()).cleanup(entry);
        }
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry();
            entry.setType(convertType(item.getString("type")));
            entry.setField(StandardField.TITLE,
                    Optional.ofNullable(item.optJSONArray("title"))
                            .map(array -> array.optString(0)).orElse(""));
            entry.setField(StandardField.SUBTITLE,
                    Optional.ofNullable(item.optJSONArray("subtitle"))
                            .map(array -> array.optString(0)).orElse(""));
            entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("author")));
            entry.setField(StandardField.YEAR,
                    Optional.ofNullable(item.optJSONObject("published-print"))
                            .map(array -> array.optJSONArray("date-parts"))
                            .map(array -> array.optJSONArray(0))
                            .map(array -> array.optInt(0))
                            .map(year -> Integer.toString(year)).orElse("")
            );
            entry.setField(StandardField.DOI, item.getString("DOI"));
            entry.setField(StandardField.PAGES, item.optString("page"));
            entry.setField(StandardField.VOLUME, item.optString("volume"));
            entry.setField(StandardField.ISSN, Optional.ofNullable(item.optJSONArray("ISSN")).map(array -> array.getString(0)).orElse(""));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("CrossRef API JSON format has changed", exception);
        }
    }

    private String toAuthors(JSONArray authors) {
        if (authors == null) {
            return "";
        }

        // input: list of {"given":"A.","family":"Riel","affiliation":[]}
        AuthorList authorsParsed = new AuthorList();
        for (int i = 0; i < authors.length(); i++) {
            JSONObject author = authors.getJSONObject(i);
            authorsParsed.addAuthor(
                    author.optString("given", ""),
                    "",
                    "",
                    author.optString("family", ""),
                    "");
        }
        return authorsParsed.getAsFirstLastNamesWithAnd();
    }

    private EntryType convertType(String type) {
        switch (type) {
            case "journal-article":
                return StandardEntryType.Article;
            default:
                return StandardEntryType.Misc;
        }
    }

    @Override
    public Optional<DOI> extractIdentifier(BibEntry inputEntry, List<BibEntry> fetchedEntries) throws FetcherException {

        final String entryTitle = REMOVE_BRACES_FORMATTER.format(inputEntry.getLatexFreeField(StandardField.TITLE).orElse(""));
        final StringSimilarity stringSimilarity = new StringSimilarity();

        for (BibEntry fetchedEntry : fetchedEntries) {
            // currently only title-based comparison
            // title
            Optional<String> dataTitle = fetchedEntry.getField(StandardField.TITLE);

            if (OptionalUtil.isPresentAnd(dataTitle, title -> stringSimilarity.isSimilar(entryTitle, title))) {
                return fetchedEntry.getDOI();
            }

            // subtitle
            // additional check, as sometimes subtitle is needed but sometimes only duplicates the title
            Optional<String> dataSubtitle = fetchedEntry.getField(StandardField.SUBTITLE);
            Optional<String> dataWithSubTitle = OptionalUtil.combine(dataTitle, dataSubtitle, (title, subtitle) -> title + " " + subtitle);
            if (OptionalUtil.isPresentAnd(dataWithSubTitle, titleWithSubtitle -> stringSimilarity.isSimilar(entryTitle, titleWithSubtitle))) {
                return fetchedEntry.getDOI();
            }
        }

        return Optional.empty();
    }

    @Override
    public String getIdentifierName() {
        return "DOI";
    }
}
