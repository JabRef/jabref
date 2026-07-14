package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.jabref.logic.util.strings.StringSimilarity;
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

/// Fetches data from the Zentralblatt Math ([zbmath.org](https://www.zbmath.org/))
@NullMarked
public class ZbMATH implements SearchBasedParserFetcher, IdBasedParserFetcher, EntryBasedParserFetcher {
    private static final Pattern NON_ALNUM_OR_SPACE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final int ENTRY_SEARCH_RESULT_COUNT = 5;
    private static final double MIN_CONFIDENCE = 0.75;
    private static final double MIN_TITLE_SIMILARITY = 0.85;
    private static final double MIN_AUTHOR_OVERLAP = 0.5;
    private static final double AMBIGUITY_DELTA = 0.05;

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
        return getURLForSearchQuery(searchQuery, String.valueOf(ENTRY_SEARCH_RESULT_COUNT));
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
        if (entry.getField(StandardField.ZBL_NUMBER).isPresent()) {
            return EntryBasedParserFetcher.super.performSearch(entry);
        }

        String searchQuery = buildSearchQuery(entry);
        if (StringUtil.isBlank(searchQuery)) {
            return List.of();
        }

        URL urlForQuery;
        try {
            urlForQuery = getURLForSearchQuery(searchQuery, String.valueOf(ENTRY_SEARCH_RESULT_COUNT));
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI is malformed", e);
        }

        List<BibEntry> candidates = getBibEntries(urlForQuery);
        return findBestMatchingEntry(entry, candidates)
                .map(List::of)
                .orElse(List.of());
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
        Object idValue = entryJson.opt("id");
        if (idValue instanceof Number || idValue instanceof String) {
            String zbmathId = String.valueOf(idValue);
            if (!StringUtil.isBlank(zbmathId)) {
                entry.withField(new UnknownField("zbmath"), zbmathId);
            }
        }

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
                    return AuthorList.parse(String.join(" and ", authorNames)).getAsLastFirstNamesWithAnd(false);
                })
                .filter(author -> !StringUtil.isBlank(author))
                .ifPresent(author -> entry.setField(StandardField.AUTHOR, author));

        Optional.ofNullable(entryJson.optJSONObject("language"))
                .map(language -> language.optJSONArray("languages"))
                .filter(languages -> !languages.isEmpty())
                .map(languages -> languages.optString(0))
                .filter(language -> !StringUtil.isBlank(language))
                .ifPresent(language -> entry.setField(StandardField.LANGUAGE, language));

        Optional.ofNullable(entryJson.optJSONObject("source"))
                .ifPresent(source -> parseSource(entry, source));

        JSONArray links = entryJson.optJSONArray("links");
        if (links != null) {
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.optJSONObject(i);
                if (link != null && "doi".equals(link.optString("type"))) {
                    Optional.of(link.optString("identifier"))
                            .filter(value -> !StringUtil.isBlank(value))
                            .ifPresent(value -> entry.withField(StandardField.DOI, value));
                    break;
                }
            }
        }

        JSONArray mscEntries = entryJson.optJSONArray("msc");
        if (mscEntries != null) {
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

        return entry;
    }

    private StandardEntryType toEntryType(JSONObject entryJson) {
        JSONObject documentType = entryJson.optJSONObject("document_type");
        if (documentType == null) {
            return StandardEntryType.Misc;
        }
        return switch (documentType.optString("code")) {
            case "j" ->
                    StandardEntryType.Article;
            case "a" ->
                    StandardEntryType.InCollection;
            case "b" ->
                    StandardEntryType.Book;
            case "p" ->
                    StandardEntryType.Unpublished;
            default ->
                    StandardEntryType.Misc;
        };
    }

    private void parseSource(BibEntry entry, JSONObject source) {
        String pages = source.optString("pages");
        if (!StringUtil.isBlank(pages)) {
            entry.setField(StandardField.PAGES, pages.replace("-", "--"));
        }

        JSONArray books = source.optJSONArray("book");
        if (books != null && !books.isEmpty()) {
            JSONObject firstBook = books.optJSONObject(0);
            if (firstBook != null) {
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
        }

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

                JSONArray issnEntries = firstSeries.optJSONArray("issn");
                if (issnEntries != null && !issnEntries.isEmpty()) {
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
            }
        }
    }

    private List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            if (e.getCause() instanceof FetcherException fetcherException) {
                throw fetcherException;
            }
            throw new FetcherException(urlForQuery, "A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForQuery, "An internal parser error occurred while fetching", e);
        }
    }

    private Optional<BibEntry> findBestMatchingEntry(BibEntry inputEntry, List<BibEntry> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        List<ScoredEntry> scoredEntries = candidates.stream()
                                                    .map(candidate -> new ScoredEntry(candidate, scoreCandidate(inputEntry, candidate)))
                                                    .filter(scoredEntry -> scoredEntry.score >= MIN_CONFIDENCE)
                                                    .sorted((left, right) -> Double.compare(right.score, left.score))
                                                    .toList();
        if (scoredEntries.isEmpty()) {
            return Optional.empty();
        }

        if (scoredEntries.size() > 1 && Math.abs(scoredEntries.get(0).score - scoredEntries.get(1).score) < AMBIGUITY_DELTA) {
            return Optional.empty();
        }

        return Optional.of(scoredEntries.get(0).entry);
    }

    private double scoreCandidate(BibEntry inputEntry, BibEntry candidate) {
        double score = 0.0;

        Optional<String> inputTitle = inputEntry.getFieldOrAlias(StandardField.TITLE)
                                                .filter(value -> !StringUtil.isBlank(value));
        if (inputTitle.isPresent()) {
            Optional<String> candidateTitle = candidate.getField(StandardField.TITLE)
                                                       .filter(value -> !StringUtil.isBlank(value));
            if (candidateTitle.isEmpty()) {
                return 0.0;
            }

            double titleSimilarity = new StringSimilarity().similarity(
                    normalizeForTitleMatching(inputTitle.get()),
                    normalizeForTitleMatching(candidateTitle.get())
            );
            if (titleSimilarity < MIN_TITLE_SIMILARITY) {
                return 0.0;
            }
            score += 0.7 * titleSimilarity;
        }

        Optional<String> inputYear = inputEntry.getFieldOrAlias(StandardField.YEAR)
                                               .filter(value -> !StringUtil.isBlank(value));
        if (inputYear.isPresent()) {
            Optional<String> candidateYear = candidate.getField(StandardField.YEAR)
                                                      .filter(value -> !StringUtil.isBlank(value));
            if (candidateYear.isEmpty() || !inputYear.get().equals(candidateYear.get())) {
                return 0.0;
            }
            score += 0.15;
        }

        Optional<String> inputAuthor = inputEntry.getFieldOrAlias(StandardField.AUTHOR)
                                                 .filter(value -> !StringUtil.isBlank(value));
        if (inputAuthor.isPresent()) {
            Optional<String> candidateAuthor = candidate.getField(StandardField.AUTHOR)
                                                        .filter(value -> !StringUtil.isBlank(value));
            if (candidateAuthor.isEmpty()) {
                return 0.0;
            }

            double authorOverlap = familyNameOverlap(inputAuthor.get(), candidateAuthor.get());
            if (authorOverlap < MIN_AUTHOR_OVERLAP) {
                return 0.0;
            }
            score += 0.15 * authorOverlap;
        }

        return score;
    }

    private double familyNameOverlap(String inputAuthors, String candidateAuthors) {
        Set<String> inputFamilyNames = AuthorList.parse(inputAuthors).getAuthors().stream()
                                                 .map(author -> author.getFamilyName().orElse(""))
                                                 .map(ZbMATH::normalizeForTitleMatching)
                                                 .filter(value -> !StringUtil.isBlank(value))
                                                 .collect(Collectors.toSet());
        if (inputFamilyNames.isEmpty()) {
            return 0.0;
        }

        Set<String> candidateFamilyNames = AuthorList.parse(candidateAuthors).getAuthors().stream()
                                                     .map(author -> author.getFamilyName().orElse(""))
                                                     .map(ZbMATH::normalizeForTitleMatching)
                                                     .filter(value -> !StringUtil.isBlank(value))
                                                     .collect(Collectors.toSet());
        long overlap = inputFamilyNames.stream()
                                       .filter(candidateFamilyNames::contains)
                                       .count();
        return (double) overlap / inputFamilyNames.size();
    }

    private static String normalizeForTitleMatching(String title) {
        String normalizedTitle = StringUtil.stripAccents(title);
        normalizedTitle = NON_ALNUM_OR_SPACE_PATTERN.matcher(normalizedTitle).replaceAll(" ");
        normalizedTitle = WHITESPACE_PATTERN.matcher(normalizedTitle).replaceAll(" ");
        return normalizedTitle.trim().toLowerCase();
    }

    private record ScoredEntry(BibEntry entry, double score) {
    }
}
