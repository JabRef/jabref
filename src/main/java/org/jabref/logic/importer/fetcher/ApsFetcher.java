package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

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

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (!doi.isPresent()) {
            return Optional.empty();
        }

        Optional<String> id = getId(doi.get().getDOI());

        if (id.isPresent()) {

            String pdfRequestUrl = PDF_URL + id.get();
            int code = Unirest.head(pdfRequestUrl).asJson().getStatus();

            if (code == 200) {
                LOGGER.info("Fulltext PDF found @ APS.");
                try {
                    return Optional.of(new URL(pdfRequestUrl));
                } catch (MalformedURLException e) {
                    LOGGER.warn("APS returned malformed URL, cannot find PDF.");
                }
            }
        }
        return Optional.empty();
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
    private Optional<String> getId(String doi) {
        // DOI is not case sensitive, but the id for the PDF URL is,
        // so we follow DOI.org redirects to get the proper id.
        // https://stackoverflow.com/a/5270162/1729441

        String doiRequest = DOI_URL + doi;

        URLConnection con;
        try {
            con = new URL(doiRequest).openConnection();
            con.connect();
            con.getInputStream();
            String[] urlParts = con.getURL().toString().split("abstract/");
            if (urlParts.length == 2) {
                return Optional.of(urlParts[1]);
            }
        } catch (IOException e) {
            LOGGER.warn("Error connecting to APS", e);
        }
        return Optional.empty();
    }
}
