package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
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
    private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("\\d{4}");

    private final ImportFormatPreferences importFormatPreferences;
    private final ImporterPreferences importerPreferences;

    public BibSonomyFetcher(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.importerPreferences = importerPreferences;
    }

    @Override
    public List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        // [impl->feat~fetchers.bibsonomy~1]
        String usernameAndApiKey = importerPreferences.getApiKey(FETCHER_NAME).map(String::strip).orElse("");
        if (!hasUsernameAndApiKey(usernameAndApiKey)) {
            throw new FetcherException(Localization.lang("BibSonomy needs your username and API key. Enter them as 'username:apikey' in Preferences > Web search > BibSonomy."));
        }
        // The transformer gives the search string and records the requested years, so the query is transformed only once.
        // BibSonomy ignores the year tag when there is no search term, so we also filter by year here.
        BibSonomyQueryTransformer transformer = new BibSonomyQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");
        return fetchEntries(query, transformer).stream()
                                               .filter(entry -> isInYearRange(entry, transformer))
                                               .toList();
    }

    private List<BibEntry> fetchEntries(String query, BibSonomyQueryTransformer transformer) throws FetcherException {
        URL url;
        try {
            url = getSearchUrl(query, transformer);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        try (InputStream stream = getUrlDownload(url).asInputStream()) {
            List<BibEntry> entries = getParser().parseEntries(stream);
            entries.forEach(this::doPostCleanup);
            return entries;
        } catch (FetcherClientException e) {
            // BibSonomy repeats the rejected username and API key in its 401 response, so that response must not reach the error message
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

    /// Entries without a four-digit year are kept, because their year cannot be compared
    private static boolean isInYearRange(BibEntry entry, BibSonomyQueryTransformer transformer) {
        return entry.getField(StandardField.YEAR)
                    .filter(year -> FOUR_DIGIT_YEAR.matcher(year).matches())
                    .map(Integer::parseInt)
                    .map(year -> transformer.getStartYear().map(startYear -> year >= startYear).orElse(true)
                            && transformer.getEndYear().map(endYear -> year <= endYear).orElse(true))
                    .orElse(true);
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        BibSonomyQueryTransformer transformer = new BibSonomyQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");
        return getSearchUrl(query, transformer);
    }

    private static URL getSearchUrl(String query, BibSonomyQueryTransformer transformer) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("resourcetype", "publication");
        uriBuilder.addParameter("format", "bibtex");
        uriBuilder.addParameter("search", query);
        getYearTag(transformer).ifPresent(yearTag -> uriBuilder.addParameter("tags", yearTag));
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
        URLDownload download = new URLDownload(url);
        importerPreferences.getApiKey(FETCHER_NAME).map(String::strip).ifPresent(key -> download.addHeader("Authorization", basicAuthHeader(key)));
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
                new FieldFormatterCleanup(StandardField.TIMESTAMP, new ClearFormatter())
        )).applySaveActions(entry);
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
            URLDownload download = new URLDownload(uriBuilder.build().toURL());
            download.addHeader("Authorization", basicAuthHeader(usernameAndApiKey));
            // openConnection() throws FetcherException for 4xx/5xx, e.g. 401 wrong key, 400 bad format
            if (download.openConnection() instanceof HttpURLConnection connection) {
                connection.disconnect();
            }
            return true;
        } catch (FetcherException | URISyntaxException | MalformedURLException _) {
            return false;
        }
    }

    /// `username:apikey` needs text on both sides of the colon
    static boolean hasUsernameAndApiKey(String key) {
        int colon = key.indexOf(':');
        return colon > 0 && colon < key.length() - 1;
    }
}
