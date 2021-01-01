package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.strings.StringSimilarity;
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

    /**
     * Hosts for which tailored fetchers exist, so this fetcher is not needed.
     */
    private final List<String> excludedHosts = Arrays.asList("link.springer.com", "ieeexplore.ieee.org");

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
            Connection connection = Jsoup.connect(doiLink);
            // pretend to be a browser (agent & referrer)
            connection.userAgent(URLDownload.USER_AGENT);
            connection.referrer("http://www.google.com");
            connection.followRedirects(true);
            connection.ignoreHttpErrors(true);
            // some publishers are quite slow (default is 3s)
            connection.timeout(10000);

            Connection.Response response = connection.execute();
            if (excludedHosts.contains(response.url().getHost())) {
                return Optional.empty();
            }

            Document html = response.parse();
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
                LOGGER.info("Fulltext PDF found @ " + doiLink);
                return Optional.of(links.get(0));
            }
            // return if links are similar or multiple links are similar
            return findSimilarLinks(links);
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

    private Optional<URL> findSimilarLinks(List<URL> urls) {
        List<URL> distinctLinks = urls.stream().distinct().collect(Collectors.toList());

        if (distinctLinks.isEmpty()) {
            return Optional.empty();
        }
        // equal
        if (distinctLinks.size() == 1) {
            return Optional.of(distinctLinks.get(0));
        }
        // similar
        final String firstElement = distinctLinks.get(0).toString();
        StringSimilarity similarity = new StringSimilarity();
        List<URL> similarLinks = distinctLinks.stream().filter(elem -> similarity.isSimilar(firstElement, elem.toString())).collect(Collectors.toList());
        if (similarLinks.size() == distinctLinks.size()) {
            return Optional.of(similarLinks.get(0));
        }

        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.SOURCE;
    }
}
