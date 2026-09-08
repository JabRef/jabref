package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.ojs.OjsSubmission;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Client for the OJS (Open Journal Systems) 3.1+ REST API, specifically the
/// `submissions` endpoint. Used to list a user's active submissions for the
/// "OJS Submission Tracker" dashboard and the Entry Editor "OJS Tracking" tab.
///
/// See <https://docs.pkp.sfu.ca/dev/api/ojs/3.4#tag/Submissions> for the endpoint
/// documentation (the response shape has drifted slightly across OJS 3.x versions;
/// [OjsSubmission#fromJSONObject] parses defensively).
///
/// Requests are authenticated with the Bearer token configured per-journal in Preferences.
public class OjsSubmissionFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OjsSubmissionFetcher.class);
    private static final String SUBMISSIONS_PATH = "api/v1/submissions";
    private static final int PAGE_SIZE = 100;

    public List<OjsSubmission> fetchSubmissions(@NonNull String journalUrl, @NonNull String apiKey) throws FetcherException {
        String journalName = extractJournalName(journalUrl);
        List<OjsSubmission> submissions = new ArrayList<>();

        URL url = buildSubmissionsUrl(journalUrl);
        URLDownload download = new URLDownload(url);
        download.addHeader("Authorization", "Bearer " + apiKey);
        download.addHeader("Accept", "application/json");

        String response = download.asString();
        JSONObject responseObject;
        try {
            responseObject = new JSONObject(response);
        } catch (JSONException e) {
            throw new FetcherException(url, "Could not parse OJS submissions response as JSON", e);
        }

        JSONArray items = responseObject.optJSONArray("items");
        if (items == null) {
            LOGGER.warn("OJS submissions response for {} did not contain an 'items' array", journalUrl);
            return submissions;
        }

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            try {
                submissions.add(OjsSubmission.fromJSONObject(item, journalName));
            } catch (JSONException e) {
                LOGGER.warn("Skipping unparsable OJS submission from {}", journalUrl, e);
            }
        }

        return submissions;
    }

    private URL buildSubmissionsUrl(String journalUrl) throws FetcherException {
        String trimmedJournalUrl = journalUrl.endsWith("/")
                                   ? journalUrl.substring(0, journalUrl.length() - 1)
                                   : journalUrl;
        try {
            URIBuilder uriBuilder = new URIBuilder(trimmedJournalUrl + "/" + SUBMISSIONS_PATH);
            uriBuilder.addParameter("count", String.valueOf(PAGE_SIZE));
            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Malformed OJS journal URL: " + journalUrl, e);
        }
    }

    /// Derives a display name for the journal from its URL path segment, e.g.
    /// `https://example.org/index.php/myjournal` -> `myjournal`.
    /// This is a placeholder until the journal's real display name is stored alongside
    /// the URL in Preferences.
    private String extractJournalName(String journalUrl) {
        String trimmedJournalUrl = journalUrl.endsWith("/")
                                   ? journalUrl.substring(0, journalUrl.length() - 1)
                                   : journalUrl;
        int lastSlash = trimmedJournalUrl.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < trimmedJournalUrl.length() - 1
               ? trimmedJournalUrl.substring(lastSlash + 1)
               : trimmedJournalUrl;
    }
}
