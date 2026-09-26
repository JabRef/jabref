package org.jabref.logic.importer.util;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class PubPeerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubPeerClient.class);
    private static final String API_URL = "https://pubpeer.com/v3/publications";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final PubPeerResponseParser parser = new PubPeerResponseParser();

    public PubPeerClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<PubPeerFeedback> fetchFeedback(List<DOI> dois) throws FetcherException {
        if (dois.isEmpty()) {
            return List.of();
        }
        if (apiKey.isBlank()) {
            throw new FetcherException("A PubPeer API key is required");
        }

        JSONObject body = new JSONObject().put("dois", dois.stream()
                                                           .map(doi -> doi.asString().toLowerCase(Locale.ROOT))
                                                           .toList());
        HttpResponse<String> response;
        try {
            response = Unirest.post(API_URL)
                              .requestTimeout((int) REQUEST_TIMEOUT.toMillis())
                              .queryString("devkey", apiKey)
                              .header("Content-Type", "application/json;charset=UTF-8")
                              .header("Accept", "application/json")
                              .body(body.toString())
                              .asString();
        } catch (UnirestException e) {
            LOGGER.debug("PubPeer request failed", e);
            throw new FetcherException("Could not retrieve PubPeer feedback", e);
        }
        if (!response.isSuccess()) {
            throw new FetcherException("PubPeer request failed with HTTP " + response.getStatus());
        }
        return parser.parse(response.getBody());
    }
}
