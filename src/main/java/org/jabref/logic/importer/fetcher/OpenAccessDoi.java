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
import kong.unirest.json.JSONObject;

/**
 * A fulltext fetcher that uses <a href="https://oadoi.org/">oaDOI</a>.
 *
 * @implSpec API is documented at http://unpaywall.org/api/v2
 */
public class OpenAccessDoi implements FulltextFetcher {
    private static String API_URL = "https://api.oadoi.org/v2/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI)
                                 .flatMap(DOI::parse);
        if (doi.isPresent()) {
            try {
                return findFullText(doi.get());
            } catch (UnirestException e) {
                throw new IOException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    public Optional<URL> findFullText(DOI doi) throws UnirestException, MalformedURLException {
        HttpResponse<JsonNode> jsonResponse = Unirest.get(API_URL + doi.getDOI() + "?email=developers@jabref.org")
                                                     .header("accept", "application/json")
                                                     .asJson();
        JSONObject root = jsonResponse.getBody().getObject();
        Optional<String> url = Optional.ofNullable(root.optJSONObject("best_oa_location"))
                .map(location -> location.optString("url"));
        if (url.isPresent()) {
            return Optional.of(new URL(url.get()));
        } else {
            return Optional.empty();
        }
    }
}
