package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.importer.MimeTypeDetector;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * FulltextFetcher implementation that follows the DOI resolution redirects and scans for a full-text PDF URL.
 */
public class DoiResolution implements FulltextFetcher {
    private static final Log LOGGER = LogFactory.getLog(DoiResolution.class);

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        Optional<DOI> doi = entry.getField(FieldName.DOI).flatMap(DOI::build);

        if(doi.isPresent()) {
            String sciLink = doi.get().getURIAsASCIIString();

            // follow all redirects and scan for a single pdf link
            if (!sciLink.isEmpty()) {
                try {
                    Connection connection = Jsoup.connect(sciLink);
                    // pretend to be a browser (agent & referrer)
                    connection.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");
                    connection.referrer("http://www.google.com");
                    connection.followRedirects(true);
                    connection.ignoreHttpErrors(true);
                    // some publishers are quite slow (default is 3s)
                    connection.timeout(5000);

                    Document html = connection.get();
                    // scan for PDF
                    Elements elements = html.body().select("a[href]");
                    List<Optional<URL>> links = new ArrayList<>();

                    for (Element element : elements) {
                        String href = element.attr("abs:href").toLowerCase(Locale.ENGLISH);
                        String hrefText = element.text().toLowerCase(Locale.ENGLISH);
                        // Only check if pdf is included in the link or inside the text
                        // ACM uses tokens without PDF inside the link
                        // See https://github.com/lehner/LocalCopy for more scrape ideas
                        if ((href.contains("pdf") || hrefText.contains("pdf")) && MimeTypeDetector.isPdfContentType(href)) {
                            links.add(Optional.of(new URL(href)));
                        }
                    }
                    // return if only one link was found (high accuracy)
                    if (links.size() == 1) {
                        LOGGER.info("Fulltext PDF found @ " + sciLink);
                        pdfLink = links.get(0);
                    }
                } catch (IOException e) {
                    LOGGER.warn("DoiResolution fetcher failed: ", e);
                }
            }
        }
        return pdfLink;
    }
}
