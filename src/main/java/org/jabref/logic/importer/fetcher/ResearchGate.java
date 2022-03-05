package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResearchGate implements FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResearchGate.class);
    private static final String GOOGLE_SEARCH = "https://www.google.com/search?q=";
    private static final String GOOGLE_SITE = "%20site:researchgate.net";

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws IOException          if an IO operation has failed
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // DOI search
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (doi.isEmpty()) {
            return Optional.empty();
        }

        // Retrieve PDF link
        Document html = Jsoup.connect(getURLByDoi(doi.get()))
                .userAgent(URLDownload.USER_AGENT)
                .ignoreHttpErrors(true)
                .get();

        Elements eLink = html.getElementsByTag("section");
        String link = eLink.select("a[href^=https]").select("a[href$=.pdf]").attr("href");
        LOGGER.info("\nGET0: " + link);

        if (link.contains("researchgate.net")) {
            return Optional.of(new URL(link));
        }
        return Optional.empty();
    }

    public String getURLByDoi(DOI doi) throws IOException {
        URIBuilder source;
        String link;
        try {
            source = new URIBuilder(GOOGLE_SEARCH + doi.getDOI() + GOOGLE_SITE);
            Document html = Jsoup.connect(source.toString())
                    .userAgent(URLDownload.USER_AGENT)
                    .ignoreHttpErrors(true)
                    .get();

            link = Objects.requireNonNull(html.getElementById("search"))
                    .select("a").attr("href");
        } catch (URISyntaxException e) {
            return null;
        }
        LOGGER.info("Source: " + link);
        return link;
    }
}

/*
  https://www.researchgate.net/publication/4207355_Paranoid_a_global_secure_file_access_control_system/citation/download
 */
