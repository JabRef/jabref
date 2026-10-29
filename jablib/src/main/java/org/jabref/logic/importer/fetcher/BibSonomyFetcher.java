package org.jabref.logic.importer.fetcher;

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
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.BibSonomyQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;

/// Searches publications on [BibSonomy](https://www.bibsonomy.org) via its REST API.
/// The API requires HTTP Basic authentication; the per-fetcher key holds `username:apikey`.
@NullMarked
public class BibSonomyFetcher implements SearchBasedParserFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "BibSonomy";

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
        // BibSonomy cannot filter by year, so the transformer only records the requested years and we filter here
        BibSonomyQueryTransformer transformer = new BibSonomyQueryTransformer();
        transformer.transformSearchQuery(queryNode);
        return SearchBasedParserFetcher.super.performSearch(queryNode).stream()
                                             .filter(entry -> isInYearRange(entry, transformer))
                                             .toList();
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
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("resourcetype", "publication");
        uriBuilder.addParameter("format", "bibtex");
        uriBuilder.addParameter("search", new BibSonomyQueryTransformer().transformSearchQuery(queryNode).orElse(""));
        uriBuilder.addParameter("start", "0");
        uriBuilder.addParameter("end", String.valueOf(MAX_RESULTS));
        return uriBuilder.build().toURL();
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
