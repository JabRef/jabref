package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fulltext fetcher that uses <a href="https://oadoi.org/">oaDOI</a>.
 *
 * @implSpec API is documented at http://unpaywall.org/api/v2
 */
public class OpenAccessDoi implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextFetcher.class);

    private static final String API_URL = "https://api.oadoi.org/v2/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI)
                                 .flatMap(DOI::parse);

        if (!doi.isPresent()) {
            return Optional.empty();
        }

        try {
            return findFullText(doi.get());
        } catch (UnirestException e) {
            throw new IOException(e);
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    public Optional<URL> findFullText(DOI doi) throws UnirestException {
        HttpResponse<JsonNode> request = Unirest.get(API_URL + doi.getDOI() + "?email=developers@jabref.org")
                                                .header("accept", "application/json")
                                                .asJson();

        return Optional.of(request)
                       .map(HttpResponse::getBody)
                       .filter(Objects::nonNull)
                       .map(JsonNode::getObject)
                       .filter(Objects::nonNull)
                       .map(root -> root.optJSONObject("best_oa_location"))
                       .filter(Objects::nonNull)
                       .map(location -> location.optString("url"))
                       .flatMap(url -> {
                           try {
                               return Optional.of(new URL(url));
                           } catch (MalformedURLException e) {
                               LOGGER.debug("Could not determine URL to fetch full text from", e);
                               return Optional.empty();
                           }
                       });
    }
}
