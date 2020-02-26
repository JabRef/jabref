package org.jabref.logic.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at APS.
 *
 * @see 'http://harvest.aps.org/docs/harvest-api'
 */
public class ApsFetcher implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApsFetcher.class);

    // The actual API needs either an API key or a header. This is a workaround.
    private static final String DOI_URL = "https://www.doi.org/";
    private static final String PDF_URL = "https://journals.aps.org/prl/pdf/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getDOI();
        if (doi.isPresent()) {
            // DOI is not case sensitive, but the id for the PDF URL is,
            // so we follow DOI.org redirects to get the proper id.
            // https://stackoverflow.com/a/5270162/1729441
            String doiRequest = DOI_URL + doi.get().getDOI();
            URLConnection con = new URL(doiRequest).openConnection();
            con.connect();
            try (InputStream is = con.getInputStream()) {
                // Do nothing, only need to open stream
            } catch (FileNotFoundException e) {
                // No such DOI
                return Optional.empty();
            }
            String id = con.getURL().toString().split("abstract/")[1];

            String pdfRequest = PDF_URL + id;
            int code = Unirest.get(pdfRequest).asJson().getStatus();

            if (code == 200) {
                LOGGER.info("Fulltext PDF found @ APS.");
                return Optional.of(new URL(pdfRequest));
            }
        }

        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }
}
