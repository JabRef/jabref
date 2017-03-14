package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.OptionalUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class for fetching DOIs from CrossRef
 *
 * See https://github.com/CrossRef/rest-api-doc
 */
public class CrossRef implements IdParserFetcher<DOI> {

    private static final Log LOGGER = LogFactory.getLog(CrossRef.class);

    private static final String API_URL = "http://api.crossref.org";

    private static final RemoveBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveBracesFormatter();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public HelpFile getHelpPage() {
        return null;
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/works");
        entry.getLatexFreeField(FieldName.TITLE).ifPresent(title -> uriBuilder.addParameter("query.title", title));
        entry.getLatexFreeField(FieldName.AUTHOR).ifPresent(author -> uriBuilder.addParameter("query.author", author));
        entry.getLatexFreeField(FieldName.YEAR).ifPresent(year ->
                uriBuilder.addParameter("filter", "from-pub-date:" + year)
        );
        uriBuilder.addParameter("rows", "20"); // = API default
        uriBuilder.addParameter("offset", "0"); // start at beginning
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONArray items = JsonReader.toJsonObject(inputStream).getJSONObject("message").getJSONArray("items");
            List<BibEntry> entries = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                BibEntry entry = jsonItemToBibEntry(item);
                entries.add(entry);
            }
            return entries;
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry();
            entry.setType(convertType(item.getString("type")));
            entry.setField(FieldName.TITLE, item.getJSONArray("title").getString(0));
            entry.setField(FieldName.SUBTITLE,
                    Optional.ofNullable(item.optJSONArray("title"))
                            .map(array -> array.optString(0)).orElse(""));
            entry.setField(FieldName.AUTHOR, toAuthors(item.getJSONArray("author")));
            entry.setField(FieldName.DATE,
                    Optional.ofNullable(item.optJSONObject("published-print"))
                            .map(array -> array.optJSONArray("date-parts"))
                            .map(array -> array.optJSONArray(0))
                            .map(array -> array.optInt(0))
                            .map(year -> Integer.toString(year)).orElse("")
            );
            entry.setField(FieldName.DOI, item.getString("DOI"));
            entry.setField(FieldName.PAGES, item.optString("page"));
            entry.setField(FieldName.VOLUME, item.optString("volume"));
            entry.setField(FieldName.ISSN, Optional.ofNullable(item.optJSONArray("ISSN")).map(array -> array.getString(0)).orElse(""));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("CrossRef API JSON format has changed", exception);
        }
    }

    private String toAuthors(JSONArray authors) {
        // input: list of {"given":"A.","family":"Riel","affiliation":[]}
        AuthorList authorsParsed = new AuthorList();
        for (int i = 0; i < authors.length(); i++) {
            JSONObject author = authors.getJSONObject(i);
            authorsParsed.addAuthor(author.getString("given"), "", "", author.getString("family"), "");
        }
        return authorsParsed.getAsFirstLastNamesWithAnd();
    }

    private EntryType convertType(String type) {
        switch (type) {
            case "journal-article":
                return BiblatexEntryTypes.ARTICLE;
            default:
                return BiblatexEntryTypes.MISC;
        }
    }

    @Override
    public Optional<DOI> extractIdentifier(BibEntry inputEntry, List<BibEntry> fetchedEntries) throws FetcherException {

        final String entryTitle = REMOVE_BRACES_FORMATTER.format(inputEntry.getLatexFreeField(FieldName.TITLE).orElse(""));
        final StringSimilarity stringSimilarity = new StringSimilarity();

        for (BibEntry fetchedEntry : fetchedEntries) {
            // currently only title-based comparison
            // title
            Optional<String> dataTitle = fetchedEntry.getField(FieldName.TITLE);

            if (OptionalUtil.isPresentAnd(dataTitle, title -> stringSimilarity.isSimilar(entryTitle, title))) {
                return fetchedEntry.getDOI();
            }

            // subtitle
            // additional check, as sometimes subtitle is needed but sometimes only duplicates the title
            Optional<String> dataSubtitle = fetchedEntry.getField(FieldName.SUBTITLE);
            Optional<String> dataWithSubTitle = OptionalUtil.combine(dataTitle, dataSubtitle, (title, subtitle) -> title + " " + subtitle);
            if (OptionalUtil.isPresentAnd(dataWithSubTitle, titleWithSubtitle -> stringSimilarity.isSimilar(entryTitle, titleWithSubtitle))) {
                return fetchedEntry.getDOI();
            }
        }

        return Optional.empty();
    }
}
