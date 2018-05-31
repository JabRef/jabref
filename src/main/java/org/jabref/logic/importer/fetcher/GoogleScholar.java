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
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at GoogleScholar.
 *
 * Search String infos: https://scholar.google.com/intl/en/scholar/help.html#searching
 */
public class GoogleScholar implements FulltextFetcher, SearchBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleScholar.class);

    private static final Pattern LINK_TO_BIB_PATTERN = Pattern.compile("(https:\\/\\/scholar.googleusercontent.com\\/scholar.bib[^\"]*)");

    private static final String BASIC_SEARCH_URL = "https://scholar.google.com/scholar?";
    private static final String SEARCH_IN_TITLE_URL = "https://scholar.google.com//scholar?";

    private static final int NUM_RESULTS = 10;

    private final ImportFormatPreferences importFormatPreferences;

    public GoogleScholar(ImportFormatPreferences importFormatPreferences) {
        Objects.requireNonNull(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Search in title
        if (!entry.hasField(FieldName.TITLE)) {
            return pdfLink;
        }

        try {
            // title search
            URIBuilder uriBuilder = new URIBuilder(SEARCH_IN_TITLE_URL);
            uriBuilder.addParameter("as_q", "");
            // as_epq as exact phrase
            uriBuilder.addParameter("as_epq", entry.getField(FieldName.TITLE).orElse(null));
            // as_occt field to search in
            uriBuilder.addParameter("as_occt", "title");

            pdfLink = search(uriBuilder.toString());
        } catch (URISyntaxException e) {
            throw new FetcherException("Building URI failed.", e);
        }

        return pdfLink;
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    private Optional<URL> search(String url) throws IOException {
        Optional<URL> pdfLink = Optional.empty();

        Document doc = Jsoup.connect(url).userAgent(URLDownload.USER_AGENT).get();
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
    public String getName() {
        return "Google Scholar";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_GOOGLE_SCHOLAR;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        try {
            obtainAndModifyCookie();
            List<BibEntry> foundEntries = new ArrayList<>(10);

            URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
            uriBuilder.addParameter("hl", "en");
            uriBuilder.addParameter("btnG", "Search");
            uriBuilder.addParameter("q", query);

            addHitsFromQuery(foundEntries, uriBuilder.toString());

            if (foundEntries.size() == 10) {
                uriBuilder.addParameter("start", "10");
                addHitsFromQuery(foundEntries, uriBuilder.toString());
            }

            return foundEntries;
        } catch (URISyntaxException e) {
            throw new FetcherException("Error while fetching from " + getName(), e);
        } catch (IOException e) {
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
    }

    private void addHitsFromQuery(List<BibEntry> entryList, String queryURL) throws IOException, FetcherException {
        String content = new URLDownload(queryURL).asString();

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
}
