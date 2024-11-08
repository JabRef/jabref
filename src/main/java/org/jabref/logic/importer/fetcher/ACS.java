package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at <a href="https://pubs.acs.org/">ACS</a>.
 */
public class ACS implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ACS.class);

    private static final String SOURCE = "https://pubs.acs.org/doi/abs/%s";

    /**
     * Tries to find a fulltext URL for a given BibTeX entry.
     * Requires the entry to have a DOI field.
     * In case no DOI is present, an empty Optional is returned.
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        String source = SOURCE.formatted(doi.get().asString());
        // Retrieve PDF link
        Document html = Jsoup.connect(source).ignoreHttpErrors(true).get();
        Element link = html.select("a.button_primary").first();

        if (link != null) {
            LOGGER.info("Fulltext PDF found @ ACS.");
            return Optional.of(URI.create(source.replaceFirst("/abs/", "/pdf/")).toURL());
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }
}
