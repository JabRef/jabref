package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at GoogleScholar.
 */
public class GoogleScholar implements FulltextFetcher, SearchBasedFetcher {
    private static final Log LOGGER = LogFactory.getLog(GoogleScholar.class);

    private static final Pattern LINK_TO_BIB_PATTERN = Pattern.compile("(https:\\/\\/scholar.googleusercontent.com\\/scholar.bib[^\"]*)");

    private static final String BASIC_SEARCH_URL = "https://scholar.google.com/scholar?hl=en&btnG=Search&oe=utf-8&q=%s";
    private static final String SEARCH_IN_TITLE_URL = "https://scholar.google.com//scholar?as_q=&as_epq=%s&as_occt=title";

    private static final int NUM_RESULTS = 10;

    private final ImportFormatPreferences importFormatPreferences;

    public GoogleScholar(ImportFormatPreferences importFormatPreferences) {
        Objects.requireNonNull(importFormatPreferences);

        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Search in title
        if (!entry.hasField(FieldName.TITLE)) {
            return pdfLink;
        }

        String url = String.format(SEARCH_IN_TITLE_URL,
                URLEncoder.encode(entry.getField(FieldName.TITLE).orElse(null), StandardCharsets.UTF_8.name()));

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0") // don't identify as a crawler
                .get();
        // Check results for PDF link
        // TODO: link always on first result or none?
        for (int i = 0; i < NUM_RESULTS; i++) {
            Elements link = doc.select(String.format("#gs_ggsW%s a", i));

            if (link.first() != null) {
                String s = link.first().attr("href");
                // link present?
                if (!"".equals(s)) {
                    // TODO: check title inside pdf + length?
                    // TODO: report error function needed?! query -> result
                    LOGGER.info("Fulltext PDF found @ Google: " + s);
                    pdfLink = Optional.of(new URL(s));
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

            String queryURL = String.format(BASIC_SEARCH_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
            String cont = URLDownload.createURLDownloadWithBrowserUserAgent(queryURL)
                    .downloadToString(StandardCharsets.UTF_8);

            Matcher m = LINK_TO_BIB_PATTERN.matcher(cont);
            while (m.find()) {
                String citationsPageURL = m.group().replace("&amp;", "&");
                BibEntry newEntry = downloadEntry(citationsPageURL);
                foundEntries.add(newEntry);
            }
            return foundEntries;
        } catch (IOException e) {
            throw new FetcherException("Fetching entries from Google Scholar failed: ", e);
        }
    }

    private BibEntry downloadEntry(String link) throws IOException, FetcherException {
        String s = URLDownload.createURLDownloadWithBrowserUserAgent(link).downloadToString(StandardCharsets.UTF_8);
        BibtexParser bp = new BibtexParser(importFormatPreferences);
        ParserResult pr = bp.parse(new StringReader(s));
        if ((pr == null) || (pr.getDatabase() == null)) {
            throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
        } else {
            Collection<BibEntry> entries = pr.getDatabase().getEntries();
            if (entries.size() != 1) {
                LOGGER.debug(entries.size() + " entries found! (" + link + ")");
                throw new FetcherException("Parsing entries from Google Scholar bib file failed.");
            } else {
                BibEntry entry = entries.iterator().next();
                entry.clearField(BibEntry.KEY_FIELD);

                return entry;
            }
        }
    }

    private void obtainAndModifyCookie() throws FetcherException {
        try {
            URLDownload downloader = URLDownload.createURLDownloadWithBrowserUserAgent("https://scholar.google.com");
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
