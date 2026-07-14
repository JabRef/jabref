package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.jabref.model.search.query.OperatorNode;
import org.jabref.model.search.query.SearchQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;

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
            throw new FetcherException("No searchable fields found for zbMATH");
        }
        return getURLForSearchQuery(searchQuery, "1");
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        String searchQuery = new ZbMathQueryTransformer()
                .transformSearchQuery(queryNode)
                .orElseThrow(() -> new URISyntaxException(String.valueOf(queryNode), "Cannot transform query for zbMATH"));
        return getURLForSearchQuery(searchQuery, "200");
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
        if (entry.getField(StandardField.ZBL_NUMBER).isEmpty() && StringUtil.isBlank(buildSearchQuery(entry))) {
            return List.of();
        }
        return EntryBasedParserFetcher.super.performSearch(entry);
    }

    private URL getURLForSearchQuery(String searchQuery, String resultCount)
            throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(DOCUMENT_SEARCH_URL);
        uriBuilder.addParameter("search_string", searchQuery);
        uriBuilder.addParameter("page", "0");
        uriBuilder.addParameter("results_per_page", resultCount);
        return uriBuilder.build().toURL();
    }

    private String buildSearchQuery(BibEntry entry) {
        List<BaseQueryNode> searchNodes = new ArrayList<>();
        entry.getFieldOrAlias(StandardField.TITLE)
             .ifPresent(title -> addSearchNode(searchNodes, StandardField.TITLE, title));
        entry.getFieldOrAlias(StandardField.AUTHOR)
             .map(AuthorList::parse)
             .ifPresent(authors -> authors.getAuthors()
                                          .forEach(author -> addSearchNode(searchNodes, StandardField.AUTHOR, author.getNamePrefixAndFamilyName())));
        entry.getFieldOrAlias(StandardField.JOURNAL)
             .ifPresent(journal -> addSearchNode(searchNodes, StandardField.JOURNAL, journal));
        entry.getFieldOrAlias(StandardField.YEAR)
             .ifPresent(year -> addSearchNode(searchNodes, StandardField.YEAR, year));
        if (searchNodes.isEmpty()) {
            return "";
        }
        return new ZbMathQueryTransformer()
                .transformSearchQuery(new OperatorNode(OperatorNode.Operator.AND, searchNodes))
                .orElse("");
    }

    private void addSearchNode(List<BaseQueryNode> searchNodes, StandardField field, String value) {
        if (!StringUtil.isBlank(value)) {
            searchNodes.add(new SearchQueryNode(Optional.of(field), value));
        }
    }

    private List<BibEntry> parseEntries(InputStream stream) throws ParseException {
        JSONObject response = JsonReader.toJsonObject(stream);
        Object result = response.opt("result");
        if (result instanceof JSONArray results) {
            List<BibEntry> entries = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject entryJson = results.optJSONObject(i);
                if (entryJson != null) {
                    entries.add(toBibEntry(entryJson));
                }
            }
            return entries;
        } else if (result instanceof JSONObject entryJson) {
            return List.of(toBibEntry(entryJson));
        }
        return List.of();
    }

    private BibEntry toBibEntry(JSONObject entryJson) {
        BibEntry entry = new BibEntry(toEntryType(entryJson));

        Optional.ofNullable(entryJson.optJSONObject("title"))
                .map(title -> title.optString("title"))
                .filter(value -> !StringUtil.isBlank(value))
                .ifPresent(value -> entry.withField(StandardField.TITLE, value));
        Optional.of(entryJson.optString("year"))
                .filter(value -> !StringUtil.isBlank(value))
                .ifPresent(value -> entry.withField(StandardField.YEAR, value));
        Optional.of(entryJson.optString("identifier"))
                .filter(value -> !StringUtil.isBlank(value))
                .ifPresent(value -> entry.withField(StandardField.ZBL_NUMBER, value));
        putInteger(entry, new UnknownField("zbmath"), entryJson, "id");
        putAuthors(entry, entryJson);
        putLanguage(entry, entryJson);
        putSource(entry, entryJson);
        putDoi(entry, entryJson);
        putMscCodes(entry, entryJson);

        return entry;
    }

    private StandardEntryType toEntryType(JSONObject entryJson) {
        JSONObject documentType = entryJson.optJSONObject("document_type");
        if (documentType == null) {
            return StandardEntryType.Misc;
        }
        return switch (documentType.optString("code")) {
            case "j" -> StandardEntryType.Article;
            case "a" -> StandardEntryType.InCollection;
            case "b" -> StandardEntryType.Book;
            case "p" -> StandardEntryType.Unpublished;
            default -> StandardEntryType.Misc;
        };
    }

    private void putAuthors(BibEntry entry, JSONObject entryJson) {
        Optional.ofNullable(entryJson.optJSONObject("contributors"))
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

    private void putSource(BibEntry entry, JSONObject entryJson) {
        Optional.ofNullable(entryJson.optJSONObject("source"))
                .ifPresent(source -> {
                    putPages(entry, source);
                    putBookSource(entry, source);
                    JSONArray series = source.optJSONArray("series");
                    if (series != null && !series.isEmpty()) {
                        JSONObject firstSeries = series.optJSONObject(0);
                        if (firstSeries != null) {
                            Optional.of(firstSeries.optString("title"))
                                    .filter(value -> !StringUtil.isBlank(value))
                                    .ifPresent(value -> entry.withField(StandardField.JOURNAL, value));
                            Optional.of(firstSeries.optString("volume"))
                                    .filter(value -> !StringUtil.isBlank(value))
                                    .ifPresent(value -> entry.withField(StandardField.VOLUME, value));
                            putIssn(entry, firstSeries);
                        }
                    }
                });
    }

    private void putBookSource(BibEntry entry, JSONObject source) {
        JSONArray books = source.optJSONArray("book");
        if (books == null || books.isEmpty()) {
            return;
        }

        JSONObject firstBook = books.optJSONObject(0);
        if (firstBook == null) {
            return;
        }

        if (entry.getType() == StandardEntryType.InCollection) {
            Optional.of(source.optString("source"))
                    .filter(value -> !StringUtil.isBlank(value))
                    .ifPresent(value -> entry.withField(StandardField.BOOKTITLE, value));
        }

        Optional.ofNullable(firstBook.optJSONArray("isbn"))
                .filter(isbnEntries -> !isbnEntries.isEmpty())
                .map(isbnEntries -> isbnEntries.optJSONObject(0))
                .ifPresent(firstIsbn -> Optional.of(firstIsbn.optString("number"))
                        .filter(value -> !StringUtil.isBlank(value))
                        .ifPresent(value -> entry.withField(StandardField.ISBN, value)));

        Optional.of(firstBook.optString("publisher"))
                .filter(value -> !StringUtil.isBlank(value))
                .ifPresent(value -> entry.withField(StandardField.PUBLISHER, value));
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
                Optional.of(issn.optString("number"))
                        .filter(value -> !StringUtil.isBlank(value))
                        .ifPresent(value -> entry.withField(StandardField.ISSN, value));
                return;
            }
        }

        JSONObject firstIssn = issnEntries.optJSONObject(0);
        if (firstIssn != null) {
            Optional.of(firstIssn.optString("number"))
                    .filter(value -> !StringUtil.isBlank(value))
                    .ifPresent(value -> entry.withField(StandardField.ISSN, value));
        }
    }

    private void putLanguage(BibEntry entry, JSONObject entryJson) {
        Optional.ofNullable(entryJson.optJSONObject("language"))
                .map(language -> language.optJSONArray("languages"))
                .filter(languages -> !languages.isEmpty())
                .map(languages -> languages.optString(0))
                .filter(language -> !StringUtil.isBlank(language))
                .ifPresent(language -> entry.setField(StandardField.LANGUAGE, language));
    }

    private void putDoi(BibEntry entry, JSONObject entryJson) {
        JSONArray links = entryJson.optJSONArray("links");
        if (links == null) {
            return;
        }

        for (int i = 0; i < links.length(); i++) {
            JSONObject link = links.optJSONObject(i);
            if (link != null && "doi".equals(link.optString("type"))) {
                Optional.of(link.optString("identifier"))
                        .filter(value -> !StringUtil.isBlank(value))
                        .ifPresent(value -> entry.withField(StandardField.DOI, value));
                return;
            }
        }
    }

    private void putMscCodes(BibEntry entry, JSONObject entryJson) {
        JSONArray mscEntries = entryJson.optJSONArray("msc");
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

    private void putInteger(BibEntry entry, UnknownField field, JSONObject source, String key) {
        if (source.has(key)) {
            entry.setField(field, String.valueOf(source.getInt(key)));
        }
    }
}
