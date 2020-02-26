package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
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
    private static final String API_URL = "https://journals.aps.org/prl/pdf/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getDOI();

        if (doi.isPresent()) {
            String request = API_URL + doi.get().getDOI();
            int code = Unirest.get(request).asJson().getStatus();

            if (code == 200) {
                LOGGER.info("Fulltext PDF found @ APS.");
                return Optional.of(new URL(request));
            }
        }

        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }
}
