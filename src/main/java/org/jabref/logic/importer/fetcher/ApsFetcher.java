package org.jabref.logic.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.model.entry.BibEntry;

import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at APS. Also see the <a
 * href="https://harvest.aps.org/docs/harvest-api">API</a>, although it isn't currently used.
 */
public class ApsFetcher implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApsFetcher.class);

    // The actual API needs either an API key or a header. This is a workaround.
    private static final String DOI_URL = "https://www.doi.org/";
    private static final String PDF_URL = "https://journals.aps.org/prl/pdf/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        return entry.getDOI().map(doi -> {
            String id;
            try {
                id = getId(doi.getDOI());
            } catch (FileNotFoundException e) {
                // No such DOI
                return null;
            } catch (IOException e) {
                // Handle IOExceptions elsewhere
                throw new UncheckedIOException(e);
            }

            String pdfRequest = PDF_URL + id;
            int code = Unirest.get(pdfRequest).asJson().getStatus();

            if (code == 200) {
                LOGGER.info("Fulltext PDF found @ APS.");
                try {
                    return new URL(pdfRequest);
                } catch (MalformedURLException e) {
                    LOGGER.info("APS returned malformed URL, cannot find PDF.");
                    return null;
                }
            } else {
                return null;
            }
        });
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    /**
     * Convert a DOI into an appropriate APS id.
     *
     * @param doi A case insensitive DOI
     * @return A DOI cased as APS likes it
     */
    private String getId(String doi) throws IOException {
        // DOI is not case sensitive, but the id for the PDF URL is,
        // so we follow DOI.org redirects to get the proper id.
        // https://stackoverflow.com/a/5270162/1729441
        String doiRequest = DOI_URL + doi;
        URLConnection con = new URL(doiRequest).openConnection();
        con.connect();

        // Throws FileNotFoundException if DOI doesn't exist
        con.getInputStream();

        // Expected URL example:
        // https://journals.aps.org/prl/abstract/10.1103/PhysRevLett.116.061102
        // Expected parts: "https://journals.aps.org/prl/", "10.1103/PhysRevLett.116.061102"
        String[] urlParts = con.getURL().toString().split("abstract/");

        if (urlParts.length != 2) {
            throw new FileNotFoundException("Unexpected URL received");
        } else {
            return urlParts[1];
        }
    }
}
