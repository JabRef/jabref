package org.jabref.logic.importer.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class PubPeerResponseParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubPeerResponseParser.class);
    private static final Pattern USER_SEPARATOR = Pattern.compile(",");
    private static final String SUCCESS = "good";

    public List<PubPeerFeedback> parse(String response) throws FetcherException {
        if (!response.stripLeading().startsWith("{")) {
            throw new FetcherException("Expected a JSON object in PubPeer response");
        }
        try {
            JSONObject root = new JSONObject(response);
            if (!SUCCESS.equals(root.getString("status"))) {
                throw new FetcherException("PubPeer reported an unsuccessful lookup");
            }
            JSONArray publications = root.getJSONArray("feedbacks");
            List<PubPeerFeedback> feedback = new ArrayList<>();
            for (int index = 0; index < publications.length(); index++) {
                feedback.add(parsePublication(publications.getJSONObject(index)));
            }
            return feedback;
        } catch (JSONException | IllegalArgumentException | UnsupportedOperationException | DateTimeException | URISyntaxException e) {
            LOGGER.debug("Invalid PubPeer response", e);
            throw new FetcherException("Invalid PubPeer response", e);
        }
    }

    private PubPeerFeedback parsePublication(JSONObject publication) throws FetcherException, URISyntaxException {
        DOI doi = DOI.parse(publication.getString("id").toLowerCase(Locale.ROOT))
                     .orElseThrow(() -> new FetcherException("Invalid DOI in PubPeer response"));
        URI url = new URI(publication.getString("url"));
        if (!"https".equalsIgnoreCase(url.getScheme()) || !("pubpeer.com".equalsIgnoreCase(url.getHost()) || "www.pubpeer.com".equalsIgnoreCase(url.getHost()))) {
            throw new FetcherException("Invalid publication URL in PubPeer response");
        }
        int totalComments = publication.getInt("total_comments");
        if (totalComments < 0) {
            throw new FetcherException("Invalid comment count in PubPeer response");
        }
        List<String> users = USER_SEPARATOR.splitAsStream(publication.getString("users"))
                                           .map(String::strip)
                                           .filter(user -> !user.isEmpty())
                                           .toList();
        return new PubPeerFeedback(doi, publication.getString("title"), url, totalComments, users, parseTimestamp(publication));
    }

    private Optional<LocalDateTime> parseTimestamp(JSONObject publication) {
        if (publication.isNull("last_commented_at")) {
            return Optional.empty();
        }
        Object value = publication.get("last_commented_at");
        String date = value instanceof JSONObject timestamp ? timestamp.getString("date") : publication.getString("last_commented_at");
        if (date.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(date.replace(' ', 'T')));
    }
}
