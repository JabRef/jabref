package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that follows the DOI resolution redirects and scans for a full-text PDF URL.
 */
public class DoiResolution implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoiResolution.class);

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (!doi.isPresent()) {
            return Optional.empty();
        }

        String doiLink = doi.get().getURIAsASCIIString();
        if (doiLink.isEmpty()) {
            return Optional.empty();
        }

        // follow all redirects and scan for a single pdf link
        try {
            Connection session = Jsoup.newSession()
                                      // some publishers are quite slow (default is 3s)
                                      .timeout(10000)
                                      .followRedirects(true)
                                      .ignoreHttpErrors(true)
                                      .referrer("https://www.google.com")
                                      .userAgent(URLDownload.USER_AGENT);

            Connection connection = session.newRequest().url(doiLink);
            Connection.Response response = connection.execute();

            Document html = response.parse();

            // citation pdf meta tag
            Optional<URL> citationMetaTag = citationMetaTag(html);
            if (citationMetaTag.isPresent()) {
                return checkPdfPresense(citationMetaTag.get(), session);
            }

            // scan for PDF
            Elements hrefElements = html.body().select("a[href]");

            List<URL> links = new ArrayList<>();
            for (Element element : hrefElements) {
                String href = element.attr("abs:href").toLowerCase(Locale.ENGLISH);
                String hrefText = element.text().toLowerCase(Locale.ENGLISH);
                // Only check if pdf is included in the link or inside the text
                // ACM uses tokens without PDF inside the link
                // See https://github.com/lehner/LocalCopy for more scrape ideas
                // link with "PDF" in title tag
                if (element.attr("title").toLowerCase(Locale.ENGLISH).contains("pdf") && new URLDownload(href).isPdf()) {
                    return Optional.of(new URL(href));
                }

                if (href.contains("pdf") || hrefText.contains("pdf") && new URLDownload(href).isPdf()) {
                    links.add(new URL(href));
                }
            }

            // return if only one link was found (high accuracy)
            if (links.size() == 1) {
                LOGGER.info("Fulltext PDF found @ {}", doiLink);
                return checkPdfPresense(links.get(0), session);
            }

            // return only if one distinct link was found
            Optional<URL> distinctLink = findDistinctLinks(links);
            if (distinctLink.isEmpty()) {
                return Optional.empty();
            }

            LOGGER.debug("Fulltext PDF link @ {}", distinctLink.get());
            return checkPdfPresense(distinctLink.get(), session);

        } catch (UnsupportedMimeTypeException type) {
            // this might be the PDF already as we follow redirects
            if (type.getMimeType().startsWith("application/pdf")) {
                return Optional.of(new URL(type.getUrl()));
            }
            LOGGER.warn("DoiResolution fetcher failed: ", type);
        } catch (IOException e) {
            LOGGER.warn("DoiResolution fetcher failed: ", e);
        }

        return Optional.empty();
    }

    /**
     * Uses the given connection to check whether there really is a PDF behind the given link
     *
     * @return Optional.empty() if there is no PDF found (but HTML)
     */
    private Optional<URL> checkPdfPresense(URL url, Connection session) throws IOException {
        // Wiley returns wrong content type
        if (url.toExternalForm().contains("pdf")) {
            // ... too much hacky ...
        }
        Connection pdfConnection = session.newRequest().url(url);
        pdfConnection.method(Connection.Method.HEAD);
        Connection.Response pdfResponse = pdfConnection.execute();
        String contentType = pdfResponse.header("Content-Type");
        if (contentType.startsWith("text/html")) {
            return Optional.empty();
        } else {
            return Optional.of(url);
        }
    }

    /**
     * Scan for <meta name="citation_pdf_url">
     * See https://scholar.google.com/intl/de/scholar/inclusion.html#indexing
     */
    private Optional<URL> citationMetaTag(Document html) {
        Elements citationPdfUrlElement = html.head().select("meta[name='citation_pdf_url']");
        Optional<String> citationPdfUrl = citationPdfUrlElement.stream().map(e -> e.attr("content")).findFirst();

        if (citationPdfUrl.isPresent()) {
            try {
                return Optional.of(new URL(citationPdfUrl.get()));
            } catch (MalformedURLException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<URL> findDistinctLinks(List<URL> urls) {
        List<URL> distinctLinks = urls.stream().distinct().collect(Collectors.toList());

        if (distinctLinks.isEmpty()) {
            return Optional.empty();
        }
        // equal
        if (distinctLinks.size() == 1) {
            return Optional.of(distinctLinks.get(0));
        }

        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.SOURCE;
    }
}
