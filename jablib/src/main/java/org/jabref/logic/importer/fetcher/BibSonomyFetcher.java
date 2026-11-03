package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.BibSonomyQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.http.SimpleHttpResponse;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Searches publications on [BibSonomy](https://www.bibsonomy.org) via its REST API.
/// The API requires HTTP Basic authentication; the per-fetcher key holds `username:apikey`.
@NullMarked
public class BibSonomyFetcher implements SearchBasedParserFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "BibSonomy";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibSonomyFetcher.class);

    private static final String SEARCH_URL = "https://www.bibsonomy.org/api/posts";
    private static final int MAX_RESULTS = 50;
    private static final List<Field> INTERNAL_FIELDS = Stream.of("added-at", "citeulike-article-id", "description", "interhash", "intrahash", "priority")
                                                             .map(FieldFactory::parseField)
                                                             .toList();

    private static final Field INTERHASH = FieldFactory.parseField("interhash");

    private final ImportFormatPreferences importFormatPreferences;
    private final ImporterPreferences importerPreferences;

    public BibSonomyFetcher(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.importerPreferences = importerPreferences;
    }

    @Override
    public List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        // [impl->feat~fetchers.bibsonomy~1]
        if (importerPreferences.getApiKey(FETCHER_NAME).map(String::strip).filter(BibSonomyFetcher::hasUsernameAndApiKey).isEmpty()) {
            throw new FetcherException(Localization.lang("BibSonomy needs your username and API key. Enter them as 'username:apikey' in Preferences > Web search > BibSonomy."));
        }
        // BibSonomy ignores the year tag when there is no search term, and the tag cannot express every query, so we also filter by year here.
        BibSonomyQueryTransformer transformer = new BibSonomyQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");
        BibSonomyYearFilter yearFilter = new BibSonomyYearFilter(queryNode);
        return fetchEntries(queryNode, query, transformer).stream()
                                                          .filter(yearFilter::accepts)
                                                          .toList();
    }

    private List<BibEntry> fetchEntries(BaseQueryNode queryNode, String query, BibSonomyQueryTransformer transformer) throws FetcherException {
        URL url;
        try {
            url = getSearchUrl(queryNode, query, transformer);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        try (InputStream stream = getUrlDownload(url).asInputStream()) {
            // The cleanup removes the interhash, so duplicates have to be removed first
            List<BibEntry> entries = removeDuplicatePosts(getParser().parseEntries(stream));
            entries.forEach(this::doPostCleanup);
            return entries;
        } catch (FetcherClientException e) {
            // BibSonomy repeats the rejected username and API key in its 401 response, so that response must reach neither the error message nor the log
            if (e.getHttpResponse().map(SimpleHttpResponse::statusCode).filter(status -> status == HttpURLConnection.HTTP_UNAUTHORIZED).isPresent()) {
                LOGGER.debug("BibSonomy rejected the username or API key");
                throw new FetcherException(Localization.lang("BibSonomy rejected the username or API key. Check them in Preferences > Web search > BibSonomy."));
            }
            throw e;
        } catch (IOException e) {
            throw new FetcherException(url, e);
        } catch (ParseException e) {
            throw new FetcherException(url, "An internal parser error occurred while fetching", e);
        }
    }

    /// BibSonomy returns one post per user, and all posts of the same publication share an `interhash`
    static List<BibEntry> removeDuplicatePosts(List<BibEntry> entries) {
        Set<String> seenHashes = new HashSet<>();
        return entries.stream()
                      .filter(entry -> entry.getField(INTERHASH).map(seenHashes::add).orElse(true))
                      .toList();
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        BibSonomyQueryTransformer transformer = new BibSonomyQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");
        return getSearchUrl(queryNode, query, transformer);
    }

    private static URL getSearchUrl(BaseQueryNode queryNode, String query, BibSonomyQueryTransformer transformer) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("resourcetype", "publication");
        uriBuilder.addParameter("format", "bibtex");
        uriBuilder.addParameter("search", query);
        if (BibSonomyYearFilter.allYearTermsAreRequired(queryNode)) {
            getYearTag(transformer).ifPresent(yearTag -> uriBuilder.addParameter("tags", yearTag));
        }
        uriBuilder.addParameter("start", "0");
        uriBuilder.addParameter("end", String.valueOf(MAX_RESULTS));
        return uriBuilder.build().toURL();
    }

    /// BibSonomy filters by year through its `sys:year` system tag: `sys:year:2015`, `sys:year:2010-2012`, `sys:year:2010-` or `sys:year:-2012`
    static Optional<String> getYearTag(BibSonomyQueryTransformer transformer) {
        Optional<Integer> startYear = transformer.getStartYear();
        Optional<Integer> endYear = transformer.getEndYear();
        if (startYear.isEmpty() && endYear.isEmpty()) {
            return Optional.empty();
        }
        if (startYear.equals(endYear)) {
            return startYear.map(year -> "sys:year:" + year);
        }
        return Optional.of("sys:year:" + startYear.map(String::valueOf).orElse("") + "-" + endYear.map(String::valueOf).orElse(""));
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        return importerPreferences.getApiKey(FETCHER_NAME)
                                  .map(String::strip)
                                  .map(key -> authenticatedDownload(url, key))
                                  .orElseGet(() -> new URLDownload(url));
    }

    /// BibSonomy repeats rejected credentials in its error responses, so these responses must not reach the log
    private static URLDownload authenticatedDownload(URL url, String usernameAndApiKey) {
        URLDownload download = new URLDownload(url);
        download.omitResponseBodyFromLog();
        download.addHeader("Authorization", basicAuthHeader(usernameAndApiKey));
        return download;
    }

    /// `username:apikey` is already the `user:password` form that HTTP Basic authentication expects
    static String basicAuthHeader(String usernameAndApiKey) {
        return "Basic " + Base64.getEncoder().encodeToString(usernameAndApiKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new DoiCleanup().cleanup(entry);
        new FieldFormatterCleanupActions(true, List.of(
                new FieldFormatterCleanup(StandardField.TIMESTAMP, new ClearFormatter()),
                new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
                new FieldFormatterCleanup(StandardField.ABSTRACT, new RemoveNewlinesFormatter())
        )).applySaveActions(entry);
        INTERNAL_FIELDS.forEach(entry::clearField);
        // BibSonomy separates tags with spaces
        entry.getField(StandardField.KEYWORDS)
             .ifPresent(tags -> entry.putKeywords(KeywordList.parse(tags, ' '), importFormatPreferences.bibEntryPreferences().getKeywordSeparator()));
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_BIBSONOMY);
    }

    @Override
    public boolean isValidKey(String apiKey) {
        String usernameAndApiKey = apiKey.strip();
        if (!hasUsernameAndApiKey(usernameAndApiKey)) {
            return false;
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
            uriBuilder.addParameter("resourcetype", "publication");
            uriBuilder.addParameter("format", "bibtex");
            uriBuilder.addParameter("start", "0");
            uriBuilder.addParameter("end", "1");
            URLDownload download = authenticatedDownload(uriBuilder.build().toURL(), usernameAndApiKey);
            // openConnection() throws FetcherException for 4xx/5xx, e.g. 401 wrong key, 400 bad format
            if (download.openConnection() instanceof HttpURLConnection connection) {
                connection.disconnect();
            }
            return true;
        } catch (FetcherException | URISyntaxException | MalformedURLException _) {
            // The message of a FetcherException contains the response, in which BibSonomy repeats the credentials, so the exception must not be logged
            LOGGER.debug("Could not validate the BibSonomy username and API key");
            return false;
        }
    }

    /// `username:apikey` needs text on both sides of the colon
    static boolean hasUsernameAndApiKey(String key) {
        int colon = key.indexOf(':');
        return colon > 0 && colon < key.length() - 1;
    }
}
