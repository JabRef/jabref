package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.transformers.ScholarQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.paging.Page;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at GoogleScholar.
 * <p>
 * Search String infos: https://scholar.google.com/intl/en/scholar/help.html#searching
 */
public class GoogleScholar implements FulltextFetcher, PagedSearchBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleScholar.class);

    private static final Pattern LINK_TO_BIB_PATTERN = Pattern.compile("(https:\\/\\/scholar.googleusercontent.com\\/scholar.bib[^\"]*)");

    private static final String BASIC_SEARCH_URL = "https://scholar.google.ch/scholar?";

    private static final int NUM_RESULTS = 10;

    private final ImportFormatPreferences importFormatPreferences;

    public GoogleScholar(ImportFormatPreferences importFormatPreferences) {
        Objects.requireNonNull(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);

        // Search in title
        if (!entry.hasField(StandardField.TITLE)) {
            return Optional.empty();
        }

        try {
            // title search
            URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
            uriBuilder.addParameter("as_q", "");
            // as_epq as exact phrase
            uriBuilder.addParameter("as_epq", entry.getField(StandardField.TITLE).orElse(""));
            // as_occt field to search in
            uriBuilder.addParameter("as_occt", "title");

            return search(uriBuilder.toString());
        } catch (URISyntaxException e) {
            throw new FetcherException("Building URI failed.", e);
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    private Optional<URL> search(String url) throws IOException {
        Optional<URL> pdfLink = Optional.empty();

        Document doc = Jsoup.connect(url).userAgent(URLDownload.USER_AGENT).get();

        if (needsCaptcha(doc.body().html())) {
            LOGGER.warn("Hit Google traffic limitation. Captcha prevents automatic fetching.");
            return Optional.empty();
        }
        // Check results for PDF link
        // TODO: link always on first result or none?
        for (int i = 0; i < NUM_RESULTS; i++) {
            Elements link = doc.select(String.format("div[data-rp=%S] div.gs_or_ggsm a", i));

            if (link.first() != null) {
                String target = link.first().attr("href");
                // link present?
                if (!target.isEmpty() && new URLDownload(target).isPdf()) {
                    // TODO: check title inside pdf + length?
                    // TODO: report error function needed?! query -> result
                    LOGGER.info("Fulltext PDF found @ Google: " + target);
                    pdfLink = Optional.of(new URL(target));
                    break;
                }
            }
        }
        return pdfLink;
    }

    private boolean needsCaptcha(String body) {
        return body.contains("id=\"gs_captcha_ccl\"");
    }

    @Override
    public String getName() {
        return "Google Scholar";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_GOOGLE_SCHOLAR);
    }

    private void addHitsFromQuery(List<BibEntry> entryList, String queryURL) throws IOException, FetcherException {
        String content = new URLDownload(queryURL).asString();

        if (needsCaptcha(content)) {
            throw new FetcherException("Fetching from Google Scholar failed: Captacha hit at " + queryURL + ".",
                    Localization.lang("This might be caused by reaching the traffic limitation of Google Scholar (see 'Help' for details)."), null);
        }

        Matcher matcher = LINK_TO_BIB_PATTERN.matcher(content);
        while (matcher.find()) {
            String citationsPageURL = matcher.group().replace("&amp;", "&");
            BibEntry newEntry = downloadEntry(citationsPageURL);
            entryList.add(newEntry);
        }
    }

    private BibEntry downloadEntry(String link) throws IOException, FetcherException {
        String downloadedContent = new URLDownload(link).asString();
        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        ParserResult result = parser.parse(new StringReader(downloadedContent));
        if ((result == null) || (result.getDatabase() == null)) {
            throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
        } else {
            Collection<BibEntry> entries = result.getDatabase().getEntries();
            if (entries.size() != 1) {
                LOGGER.debug(entries.size() + " entries found! (" + link + ")");
                throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
            } else {
                BibEntry entry = entries.iterator().next();
                return entry;
            }
        }
    }

    private void obtainAndModifyCookie() throws FetcherException {
        try {
            URLDownload downloader = new URLDownload("https://scholar.google.com");
            List<HttpCookie> cookies = downloader.getCookieFromUrl();
            for (HttpCookie cookie : cookies) {
                // append "CF=4" which represents "Citation format bibtex"
                cookie.setValue(cookie.getValue() + ":CF=4");
            }
        } catch (IOException e) {
            throw new FetcherException("Cookie configuration for Google Scholar failed.", e);
        }
    }

    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {
        ScholarQueryTransformer queryTransformer = new ScholarQueryTransformer();
        String transformedQuery = queryTransformer.transformLuceneQuery(luceneQuery).orElse("");
        try {
            obtainAndModifyCookie();
            List<BibEntry> foundEntries = new ArrayList<>(10);
            URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
            uriBuilder.addParameter("hl", "en");
            uriBuilder.addParameter("btnG", "Search");
            uriBuilder.addParameter("q", transformedQuery);
            uriBuilder.addParameter("start", String.valueOf(pageNumber * getPageSize()));
            uriBuilder.addParameter("num", String.valueOf(getPageSize()));
            uriBuilder.addParameter("as_ylo", String.valueOf(queryTransformer.getStartYear()));
            uriBuilder.addParameter("as_yhi", String.valueOf(queryTransformer.getEndYear()));

            try {
                addHitsFromQuery(foundEntries, uriBuilder.toString());

                if (foundEntries.size() == 10) {
                    uriBuilder.addParameter("start", "10");
                    addHitsFromQuery(foundEntries, uriBuilder.toString());
                }
            } catch (IOException e) {
                LOGGER.info("IOException for URL {}", uriBuilder.toString());
                // if there are too much requests from the same IP adress google is answering with a 503 and redirecting to a captcha challenge
                // The caught IOException looks for example like this:
                // java.io.IOException: Server returned HTTP response code: 503 for URL: https://ipv4.google.com/sorry/index?continue=https://scholar.google.com/scholar%3Fhl%3Den%26btnG%3DSearch%26q%3Dbpmn&hl=en&q=CGMSBI0NBDkYuqy9wAUiGQDxp4NLQCWbIEY1HjpH5zFJhv4ANPGdWj0
                if (e.getMessage().contains("Server returned HTTP response code: 503 for URL")) {
                    throw new FetcherException("Fetching from Google Scholar failed.",
                            Localization.lang("This might be caused by reaching the traffic limitation of Google Scholar (see 'Help' for details)."), e);
                } else {
                    throw new FetcherException("Error while fetching from " + getName(), e);
                }
            }
            return new Page<>(transformedQuery, pageNumber, foundEntries);
        } catch (URISyntaxException e) {
            throw new FetcherException("Error while fetching from " + getName(), e);
        }
    }
}
