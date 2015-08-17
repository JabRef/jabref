package net.sf.jabref.logic.fetcher;

import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL from a GoogleScholar search page.
 */
public class GoogleScholar implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(GoogleScholar.class);

    private static final String SEARCH_URL = "https://scholar.google.com//scholar?as_q=&as_epq=%s&as_occt=title";
    private static final int NUM_RESULTS = 10;

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Search in title
        String entryTitle = entry.getField("title");

        if (entryTitle == null) {
            return pdfLink;
        }

        String url = String.format(SEARCH_URL, URLEncoder.encode(entryTitle, "UTF-8"));

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla") // don't identify as a crawler FIXME: still gets blocked in tests
                .get();
        // Check results for PDF link
        // TODO: link always on first result or none?
        for (int i = 0; i < NUM_RESULTS; i++) {
            Elements link = doc.select(String.format("#gs_ggsW%s a", i));

            if (link.first() != null) {
                String s = link.first().attr("href");
                // link present?
                if (!s.equals("")) {
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
}
