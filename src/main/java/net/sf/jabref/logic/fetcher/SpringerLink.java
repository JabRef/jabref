package net.sf.jabref.logic.fetcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.net.URL;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at SpringerLink.
 *
 * Uses Springer API, see @link{https://dev.springer.com}
 */
public class SpringerLink implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(SpringerLink.class);

    private static final String API_URL = "http://api.springer.com/meta/v1/json";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final String CONTENT_HOST = "link.springer.com";

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            // Available in catalog?
            try {
                HttpResponse<JsonNode> jsonResponse = Unirest.get(API_URL)
                        .queryString("api_key", API_KEY)
                        .queryString("q", String.format("doi:%s", doi.get().getDOI()))
                        .asJson();

                JSONObject json = jsonResponse.getBody().getObject();
                int results = json.getJSONArray("result").getJSONObject(0).getInt("total");

                if (results > 0) {
                    LOGGER.info("Fulltext PDF found @ Springer.");
                    pdfLink = Optional.of(new URL("http", CONTENT_HOST, String.format("/content/pdf/%s.pdf", doi.get().getDOI())));
                }
            } catch(UnirestException e) {
                LOGGER.warn("SpringerLink API request failed: " + e.getMessage());
            }
        }

        // TODO: title search
        // We can also get abstract automatically!
        return pdfLink;
    }
}
