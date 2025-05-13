package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at SpringerLink.
 * <p>
 * Uses Springer API, see <a href="https://dev.springer.com">https://dev.springer.com</a>
 */
public class SpringerLink implements FulltextFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Springer";

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringerLink.class);

    private static final String API_URL = "https://api.springer.com/meta/v1/json";
    private static final String CONTENT_HOST = "link.springer.com";

    private final ImporterPreferences importerPreferences;

    public SpringerLink(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // Try unique DOI first
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (doi.isEmpty()) {
            return Optional.empty();
        }
        // Available in catalog?
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.get(API_URL)
                                                         .queryString("api_key", importerPreferences.getApiKey(getName()).orElse(""))
                                                         .queryString("q", "doi:%s".formatted(doi.get().asString()))
                                                         .asJson();
            if (jsonResponse.getBody() != null) {
                JSONObject json = jsonResponse.getBody().getObject();
                int results = json.getJSONArray("result").getJSONObject(0).getInt("total");

                if (results > 0) {
                    LOGGER.info("Fulltext PDF found @ Springer.");
                    return Optional.of(new URI("http", null, CONTENT_HOST, -1, "/content/pdf/%s.pdf".formatted(doi.get().asString()), null, null).toURL());
                }
            }
        } catch (UnirestException | URISyntaxException e) {
            LOGGER.warn("SpringerLink API request failed", e);
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
