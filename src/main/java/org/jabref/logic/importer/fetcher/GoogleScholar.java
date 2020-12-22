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

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.web.WebView;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.paging.Page;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.sun.star.sheet.XSolver;
import kong.unirest.Unirest;
import org.apache.http.client.utils.URIBuilder;
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

    private static final Pattern LINK_TO_SUBPAGE_PATTERN = Pattern.compile("data-clk-atid=\"([^\"]*)\"");
    private static final Pattern LINK_TO_BIB_PATTERN = Pattern.compile("(https:\\/\\/scholar.googleusercontent.com\\/scholar.bib[^\"]*)");

    private static final String BASIC_SEARCH_URL = "https://scholar.google.ch/scholar?";

    private static final int NUM_RESULTS = 10;

    private final ImportFormatPreferences importFormatPreferences;
    private CaptchaSolver captchaSolver;

    public GoogleScholar(ImportFormatPreferences importFormatPreferences, CaptchaSolver solver) {
        Objects.requireNonNull(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
        this.captchaSolver = solver;
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

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    @Override
    public String getName() {
        return "Google Scholar";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_GOOGLE_SCHOLAR);
    }

    @Override
    public Page<BibEntry> performSearchPaged(ComplexSearchQuery complexSearchQuery, int pageNumber) throws FetcherException {
        LOGGER.debug("Using query {}", complexSearchQuery);
        List<BibEntry> foundEntries = new ArrayList<>(getPageSize());

        String complexQueryString = constructComplexQueryString(complexSearchQuery);
        final URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
        } catch (URISyntaxException e) {
            throw new FetcherException("Error while fetching from " + getName(), e);
        }
        uriBuilder.addParameter("hl", "en");
        uriBuilder.addParameter("btnG", "Search");
        uriBuilder.addParameter("q", complexQueryString);
        uriBuilder.addParameter("start", String.valueOf(pageNumber * getPageSize()));
        uriBuilder.addParameter("num", String.valueOf(getPageSize()));
        complexSearchQuery.getFromYear().ifPresent(year -> uriBuilder.addParameter("as_ylo", year.toString()));
        complexSearchQuery.getToYear().ifPresent(year -> uriBuilder.addParameter("as_yhi", year.toString()));
        complexSearchQuery.getSingleYear().ifPresent(year -> {
            uriBuilder.addParameter("as_ylo", year.toString());
            uriBuilder.addParameter("as_yhi", year.toString());
        });

        String queryURL = uriBuilder.toString();
        LOGGER.debug("Using URL {}", queryURL);
        try {
            addHitsFromQuery(foundEntries, queryURL);
        } catch (IOException e) {
            LOGGER.info("IOException for URL {}", queryURL);
            // If there are too much requests from the same IP address google is answering with a 403, 429, or 503 and redirecting to a captcha challenge
            // Example URL: https://www.google.com/sorry/index?continue=https://scholar.google.ch/scholar%3Fhl%3Den%26btnG%3DSearch%26q%3D%2522in%2522%2B%2522and%2522%2B%2522Process%2522%2B%2522Models%2522%2B%2522Issues%2522%2B%2522Interoperability%2522%2B%2522Detecting%2522%2B%2522Correctness%2522%2B%2522BPMN%2522%2B%25222.0%2522%2Ballintitle%253A%26start%3D0%26num%3D20&hl=en&q=EgTZGO7HGOuK2P4FIhkA8aeDSwDHMafs3bst5vlLM-Sk4TtpMrOtMgFy
            // The caught IOException looks for example like this:
            // java.io.IOException: Server returned HTTP response code: 503 for URL: https://ipv4.google.com/sorry/index?continue=https://scholar.google.com/scholar%3Fhl%3Den%26btnG%3DSearch%26q%3Dbpmn&hl=en&q=CGMSBI0NBDkYuqy9wAUiGQDxp4NLQCWbIEY1HjpH5zFJhv4ANPGdWj0
            if (e.getMessage().contains("Server returned HTTP response code: 403 for URL") ||
                    e.getMessage().contains("Server returned HTTP response code: 429 for URL") ||
                    e.getMessage().contains("Server returned HTTP response code: 503 for URL")) {
                LOGGER.debug("Captcha found. Calling the CaptchaSolver");
                String content = captchaSolver.solve(queryURL);
                LOGGER.debug("Returned result {}", content);
                try {
                    extractEntriesFromContent(content, foundEntries);
                } catch (IOException ioException) {
                    LOGGER.error("Still failing at Google Scholar", ioException);
                }
                throw new FetcherException("Fetching from Google Scholar failed.",
                        Localization.lang("This might be caused by reaching the traffic limitation of Google Scholar (see 'Help' for details)."), e);
            } else {
                throw new FetcherException("Error while fetching from " + getName(), e);
            }
        }
        return new Page<>(complexQueryString, pageNumber, foundEntries);
    }

    private String constructComplexQueryString(ComplexSearchQuery complexSearchQuery) {
        List<String> searchTerms = new ArrayList<>(complexSearchQuery.getDefaultFieldPhrases());
        complexSearchQuery.getAuthors().forEach(author -> searchTerms.add("author:" + author));
        if (!complexSearchQuery.getTitlePhrases().isEmpty()) {
            searchTerms.add("allintitle:" + String.join(" ", complexSearchQuery.getTitlePhrases()));
        }
        complexSearchQuery.getJournal().ifPresent(journal -> searchTerms.add("source:" + journal));
        // API automatically ANDs the terms
        return String.join(" ", searchTerms);
    }

    private void addHitsFromQuery(List<BibEntry> entryList, String queryURL) throws IOException, FetcherException {
        LOGGER.debug("Downloading from {}", queryURL);
        URLDownload urlDownload = new URLDownload(queryURL);
        obtainAndModifyCookie(urlDownload);

        String content = urlDownload.asString();
        if (needsCaptcha(content)) {
            throw new FetcherException("Fetching from Google Scholar failed: Captcha hit at " + queryURL + ".",
                    Localization.lang("This might be caused by reaching the traffic limitation of Google Scholar (see 'Help' for details)."), null);
        }

        extractEntriesFromContent(content, entryList);
    }

    private void extractEntriesFromContent(String content, List<BibEntry> entryList) throws IOException, FetcherException {
        Matcher matcher = LINK_TO_SUBPAGE_PATTERN.matcher(content);
        if (!matcher.find()) {
            LOGGER.debug("No data-clk-atid found in html {}", content);
            return;
        }

        String infoPageUrl = BASIC_SEARCH_URL + "q=info:" + matcher.group(1) + ":scholar.google.com/&output=cite&scirp=0&hl=en";
        LOGGER.debug("Using infoPageUrl {}", infoPageUrl);
        // FIXME: Existing cookies should be reused.
        URLDownload infoPageUrlDownload = new URLDownload(infoPageUrl);
        LOGGER.debug("Downloading from {}", infoPageUrl);
        String infoPageContent = infoPageUrlDownload.asString();

        matcher = LINK_TO_BIB_PATTERN.matcher(infoPageContent);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String citationsPageURL = matcher.group().replace("&amp;", "&");
            LOGGER.debug("Using citationsPageURL {}", citationsPageURL);
            BibEntry newEntry = downloadEntry(citationsPageURL);
            entryList.add(newEntry);
        }
        if (!found) {
            LOGGER.debug("Did not found pattern in html {}", infoPageContent);
        }
    }

    private BibEntry downloadEntry(String link) throws IOException, FetcherException {
        LOGGER.debug("Downloading from {}", link);
        String downloadedContent = new URLDownload(link).asString();
        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        ParserResult result = parser.parse(new StringReader(downloadedContent));
        if ((result == null) || (result.getDatabase() == null)) {
            throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
        } else {
            Collection<BibEntry> entries = result.getDatabase().getEntries();
            if (entries.size() != 1) {
                LOGGER.debug("{} entries found ({})", entries.size(), link);
                throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
            } else {
                BibEntry entry = entries.iterator().next();
                return entry;
            }
        }
    }

    private void obtainAndModifyCookie(URLDownload downloader) throws FetcherException {
        try {
            List<HttpCookie> cookies = downloader.getCookieFromUrl();
            for (HttpCookie cookie : cookies) {
                // append "CF=4" which represents "Citation format bibtex"
                cookie.setValue(cookie.getValue() + ":CF=4");
            }
        } catch (IOException e) {
            throw new FetcherException("Cookie configuration for Google Scholar failed.", e);
        }
    }

    public void displayCaptchaDialog(String link) {
        Platform.runLater(() -> new CaptchaDialog(link).showAndWait());
        /*
        if (dialog.retry()) {
            displayCaptchaDialog(link);
        }
        */
    }

    private boolean needsCaptcha(String body) {
        return body.contains("id=\"gs_captcha_ccl\"");
    }

    private static final class CaptchaDialog extends BaseDialog<Void> {
        public CaptchaDialog(String content) {
            super();
            this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            this.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(true);
            WebView webView = new WebView();

            // webView.getEngine().setJavaScriptEnabled(true);
            webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0");
            this.getDialogPane().setContent(webView);
            webView.getEngine().loadContent(content);
        }

        public boolean retry() {
            return false;
        }
    }
}
