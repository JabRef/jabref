package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.EPRINT;

public class SemanticScholar implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticScholar.class);

    private static final String SOURCE = "https://api.semanticscholar.org/v1/paper/%s";

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     * <p>
     * Currently, only uses the DOI or arXiv identifier if found.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws IOException if one of the URL is not correct
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        Optional<ArXivIdentifier> arXiv = entry.getField(EPRINT).flatMap(ArXivIdentifier::parse);

        Document html = null;
        if (doi.isPresent()) {
            try {
                // Retrieve PDF link
                String source = String.format(SOURCE, doi.get().getDOI());
                html = Jsoup.connect(getURLBySource(source))
                        .userAgent(URLDownload.USER_AGENT)
                        .referrer("https://www.google.com")
                        .ignoreHttpErrors(true)
                        .get();
            } catch (IOException e) {
                LOGGER.info("Error for pdf lookup with DOI");
            }
        }
        if (arXiv.isPresent() && entry.getField(EPRINT).isPresent()) {
            // Check if entry is a match
            String arXivString = entry.getField(EPRINT).get();
            if (!arXivString.startsWith("arXiv:")) {
                arXivString = "arXiv:" + arXivString;
            }
            String source = String.format(SOURCE, arXivString);
            html = Jsoup.connect(getURLBySource(source))
                    .userAgent(URLDownload.USER_AGENT)
                    .referrer("https://www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
        }
        if (html == null) {
            return Optional.empty();
        }

        // Retrieve PDF link from button on the webpage
        // First checked is a drop-down menu, as it has the correct URL if present
        Elements metaLinks = html.getElementsByClass("flex-item alternate-sources__dropdown");
        String link = metaLinks.select("a").attr("href");
        if (link.length() < 10) {
            metaLinks = html.getElementsByClass("flex-paper-actions__button--primary");
            link = metaLinks.select("a").attr("href");
        }
        LOGGER.info("Fulltext PDF found @ SemanticScholar.");
        return Optional.of(new URL(link));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    /**
     * Finds the URL for the webpage to look for the link to download the pdf.
     *
     * @param source the API link that contains the relevant information.
     * @return the correct URL
     * @throws IOException if error by downloading the page
     */
    public String getURLBySource(String source) throws IOException {
        URLDownload download = new URLDownload(source);
        JSONObject json = new JSONObject(download.asString());
        return json.get("url").toString();
    }
}
