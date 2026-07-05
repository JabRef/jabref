package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.ZbMathQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Fetches data from the Zentralblatt Math (https://www.zbmath.org/)
@NullMarked
public class ZbMATH implements SearchBasedParserFetcher, IdBasedParserFetcher, EntryBasedParserFetcher {

    // The zbMATH REST document API is documented at:
    // https://github.com/zbMATHOpen/zbRestApiClient/blob/master/docs/DocumentApi.md
    private static final String DOCUMENT_SEARCH_URL =
            "https://api.zbmath.org/v1/document/_search";
    private static final String DOCUMENT_URL =
            "https://api.zbmath.org/v1/document/";

    private final ImportFormatPreferences preferences;

    public ZbMATH(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "zbMATH";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ZBMATH);
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        Optional<String> zblidInEntry = entry.getField(StandardField.ZBL_NUMBER);
        if (zblidInEntry.isPresent()) {
            // zbmath id is already present
            return getUrlForIdentifier(zblidInEntry.get());
        }

        String searchQuery = buildSearchQuery(entry);
        if (StringUtil.isBlank(searchQuery)) {
            throw new ZbMathNoUrlException("No searchable fields found for zbMATH");
        }
        return getURLForDocumentSearch(searchQuery, "1");
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        String searchQuery = new ZbMathQueryTransformer().transformSearchQuery(queryNode).orElse("");
        return getURLForDocumentSearch(searchQuery, "200");
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(DOCUMENT_URL.concat(identifier));
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return this::parseEntries;
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup(new UnknownField("msc2010"), StandardField.KEYWORDS).cleanup(entry);
        new MoveFieldCleanup(AMSField.FJOURNAL, StandardField.JOURNAL).cleanup(entry);
        new FieldFormatterCleanup(StandardField.JOURNAL, new RemoveEnclosingBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveEnclosingBracesFormatter()).cleanup(entry);
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        try {
            return EntryBasedParserFetcher.super.performSearch(entry);
        } catch (ZbMathNoUrlException e) {
            return List.of();
        }
    }

    public static class ZbMathNoUrlException extends FetcherException {
        public ZbMathNoUrlException(String errorMessage) {
            super(errorMessage);
        }
    }

    private URL getURLForDocumentSearch(String searchQuery, String resultCount)
            throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(DOCUMENT_SEARCH_URL);
        uriBuilder.addParameter("search_string", searchQuery);
        uriBuilder.addParameter("page", "0");
        uriBuilder.addParameter("results_per_page", resultCount);
        return uriBuilder.build().toURL();
    }

    private String buildSearchQuery(BibEntry entry) {
        StringJoiner searchQuery = new StringJoiner(" ");
        entry.getFieldOrAlias(StandardField.TITLE)
             .ifPresent(title -> addExactSearchTerm(searchQuery, "ti", title));
        entry.getFieldOrAlias(StandardField.AUTHOR)
             .map(AuthorList::parse)
             .ifPresent(authors -> authors.getAuthors()
                                          .forEach(author -> addExactSearchTerm(searchQuery, "au", author.getNamePrefixAndFamilyName())));
        entry.getFieldOrAlias(StandardField.JOURNAL)
             .ifPresent(journal -> addExactSearchTerm(searchQuery, "so", journal));
        entry.getFieldOrAlias(StandardField.YEAR)
             .ifPresent(year -> addSearchTerm(searchQuery, "py", year));
        return searchQuery.toString();
    }

    private void addExactSearchTerm(StringJoiner searchQuery, String field, String value) {
        if (!StringUtil.isBlank(value)) {
            addSearchTerm(searchQuery, field, "\"%s\"".formatted(value.replace("\"", "\\\"")));
        }
    }

    private void addSearchTerm(StringJoiner searchQuery, String field, String value) {
        if (!StringUtil.isBlank(value)) {
            searchQuery.add("%s:%s".formatted(field, value));
        }
    }

    private List<BibEntry> parseEntries(InputStream stream) throws ParseException {
        JSONObject response = JsonReader.toJsonObject(stream);
        Object result = response.opt("result");
        if (result instanceof JSONArray results) {
            List<BibEntry> entries = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject document = results.optJSONObject(i);
                if (document != null) {
                    entries.add(toBibEntry(document));
                }
            }
            return entries;
        } else if (result instanceof JSONObject document) {
            return List.of(toBibEntry(document));
        }
        return List.of();
    }

    private BibEntry toBibEntry(JSONObject document) {
        BibEntry entry = new BibEntry(toEntryType(document));

        putString(entry, StandardField.TITLE, document.optJSONObject("title"), "title");
        putString(entry, StandardField.YEAR, document, "year");
        putString(entry, StandardField.ZBL_NUMBER, document, "identifier");
        putInteger(entry, new UnknownField("zbmath"), document, "id");
        putAuthors(entry, document);
        putLanguage(entry, document);
        putSource(entry, document);
        putDoi(entry, document);
        putMscCodes(entry, document);

        return entry;
    }

    private StandardEntryType toEntryType(JSONObject document) {
        JSONObject documentType = document.optJSONObject("document_type");
        if (documentType != null && "j".equals(documentType.optString("code"))) {
            return StandardEntryType.Article;
        }
        return StandardEntryType.Misc;
    }

    private void putAuthors(BibEntry entry, JSONObject document) {
        Optional.ofNullable(document.optJSONObject("contributors"))
                .map(contributors -> contributors.optJSONArray("authors"))
                .map(authors -> {
                    List<String> authorNames = new ArrayList<>();
                    for (int i = 0; i < authors.length(); i++) {
                        JSONObject author = authors.optJSONObject(i);
                        if (author != null && !StringUtil.isBlank(author.optString("name"))) {
                            authorNames.add(author.optString("name"));
                        }
                    }
                    return String.join(" and ", authorNames);
                })
                .filter(author -> !StringUtil.isBlank(author))
                .ifPresent(author -> entry.setField(StandardField.AUTHOR, author));
    }

    private void putSource(BibEntry entry, JSONObject document) {
        Optional.ofNullable(document.optJSONObject("source"))
                .ifPresent(source -> {
                    putPages(entry, source);
                    JSONArray series = source.optJSONArray("series");
                    if (series != null && !series.isEmpty()) {
                        JSONObject firstSeries = series.optJSONObject(0);
                        if (firstSeries != null) {
                            putString(entry, StandardField.JOURNAL, firstSeries, "title");
                            putString(entry, StandardField.VOLUME, firstSeries, "volume");
                            putIssn(entry, firstSeries);
                        }
                    }
                });
    }

    private void putPages(BibEntry entry, JSONObject source) {
        String pages = source.optString("pages");
        if (!StringUtil.isBlank(pages)) {
            entry.setField(StandardField.PAGES, pages.replace("-", "--"));
        }
    }

    private void putIssn(BibEntry entry, JSONObject series) {
        JSONArray issnEntries = series.optJSONArray("issn");
        if (issnEntries == null || issnEntries.isEmpty()) {
            return;
        }

        for (int i = 0; i < issnEntries.length(); i++) {
            JSONObject issn = issnEntries.optJSONObject(i);
            if (issn != null && "print".equals(issn.optString("type"))) {
                putString(entry, StandardField.ISSN, issn, "number");
                return;
            }
        }

        JSONObject firstIssn = issnEntries.optJSONObject(0);
        if (firstIssn != null) {
            putString(entry, StandardField.ISSN, firstIssn, "number");
        }
    }

    private void putLanguage(BibEntry entry, JSONObject document) {
        Optional.ofNullable(document.optJSONObject("language"))
                .map(language -> language.optJSONArray("languages"))
                .filter(languages -> !languages.isEmpty())
                .map(languages -> languages.optString(0))
                .filter(language -> !StringUtil.isBlank(language))
                .ifPresent(language -> entry.setField(StandardField.LANGUAGE, language));
    }

    private void putDoi(BibEntry entry, JSONObject document) {
        JSONArray links = document.optJSONArray("links");
        if (links == null) {
            return;
        }

        for (int i = 0; i < links.length(); i++) {
            JSONObject link = links.optJSONObject(i);
            if (link != null && "doi".equals(link.optString("type"))) {
                putString(entry, StandardField.DOI, link, "identifier");
                return;
            }
        }
    }

    private void putMscCodes(BibEntry entry, JSONObject document) {
        JSONArray mscEntries = document.optJSONArray("msc");
        if (mscEntries == null) {
            return;
        }

        List<String> mscCodes = new ArrayList<>();
        for (int i = 0; i < mscEntries.length(); i++) {
            JSONObject msc = mscEntries.optJSONObject(i);
            if (msc != null && !StringUtil.isBlank(msc.optString("code"))) {
                mscCodes.add(msc.optString("code"));
            }
        }
        if (!mscCodes.isEmpty()) {
            String separator = preferences.bibEntryPreferences().getKeywordSeparator() + "";
            entry.setField(StandardField.KEYWORDS, String.join(separator, mscCodes));
        }
    }

    private void putString(BibEntry entry, StandardField field, @Nullable JSONObject source, String key) {
        if (source == null) {
            return;
        }

        String value = source.optString(key);
        if (!StringUtil.isBlank(value)) {
            entry.setField(field, value);
        }
    }

    private void putInteger(BibEntry entry, UnknownField field, JSONObject source, String key) {
        if (source.has(key)) {
            entry.setField(field, String.valueOf(source.getInt(key)));
        }
    }
}
