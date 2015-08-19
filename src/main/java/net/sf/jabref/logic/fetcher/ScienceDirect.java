package net.sf.jabref.logic.fetcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at ScienceDirect.
 *
 * @see http://dev.elsevier.com/
 */
public class ScienceDirect implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ScienceDirect.class);

    private static final String API_URL = "http://api.elsevier.com/content/article/doi/";
    private static final String API_KEY = "fb82f2e692b3c72dafe5f4f1fa0ac00b";
    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            // Available in catalog?
            try {
                String request = API_URL + doi.get().getDOI();
                HttpResponse<InputStream> response = Unirest.get(request)
                        .header("X-ELS-APIKey", API_KEY)
                        .queryString("httpAccept", "application/pdf")
                        .asBinary();

                if (response.getStatus() == 200) {
                    LOGGER.info("Fulltext PDF found @ ScienceDirect.");
                    pdfLink = Optional.of(new URL(request + "?httpAccept=application/pdf"));
                }
            } catch(UnirestException e) {
                LOGGER.warn("Elsevier API request failed: " + e.getMessage());
            }
        }

        // TODO: title search
        // We can also get abstract automatically!
        return pdfLink;
    }
}
