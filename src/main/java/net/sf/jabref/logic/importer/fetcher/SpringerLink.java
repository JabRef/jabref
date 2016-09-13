package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at SpringerLink.
 *
 * Uses Springer API, see @link{https://dev.springer.com}
 */
public class SpringerLink implements FulltextFetcher {
    private static final Log LOGGER = LogFactory.getLog(SpringerLink.class);

    private static final String API_URL = "http://api.springer.com/meta/v1/json";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final String CONTENT_HOST = "link.springer.com";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = entry.getField(FieldName.DOI).flatMap(DOI::build);

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
                LOGGER.warn("SpringerLink API request failed", e);
            }
        }
        return pdfLink;
    }
}
