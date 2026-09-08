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
import org.jspecify.annotations.NullMarked;
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
@NullMarked
public class OjsSubmissionFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OjsSubmissionFetcher.class);
    private static final String SUBMISSIONS_PATH = "api/v1/submissions";
    private static final int PAGE_SIZE = 100;
    // Hard ceiling on pages fetched per call, independent of what the server reports as
    // itemsMax, so a malformed/malicious response can never make this loop indefinitely.
    private static final int MAX_PAGES = 50;

    public List<OjsSubmission> fetchSubmissions(String journalUrl, String apiKey) throws FetcherException {
        String journalName = extractJournalName(journalUrl);
        List<OjsSubmission> submissions = new ArrayList<>();

        int offset = 0;
        int itemsMax = Integer.MAX_VALUE;
        for (int page = 0; page < MAX_PAGES && offset < itemsMax; page++) {
            URL url = buildSubmissionsUrl(journalUrl, offset);
            URLDownload download = new URLDownload(url);
            download.addHeader("Authorization", "Bearer " + apiKey);
            download.addHeader("Accept", "application/json");

            String response = download.asString();
            JSONObject responseObject;
            try {
                responseObject = new JSONObject(response);
            } catch (JSONException e) {
                LOGGER.warn("Could not parse OJS submissions response from {} as JSON", url, e);
                throw new FetcherException(url, "Could not parse OJS submissions response as JSON", e);
            }

            itemsMax = responseObject.optInt("itemsMax", -1);

            JSONArray items = responseObject.optJSONArray("items");
            if (items == null || items.isEmpty()) {
                if (itemsMax < 0) {
                    LOGGER.warn("OJS submissions response for {} did not contain an 'items' array", journalUrl);
                }
                break;
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

            offset += items.length();
            if (itemsMax < 0) {
                // Server did not report a total; stop after one page rather than guessing.
                break;
            }
        }

        return submissions;
    }

    private URL buildSubmissionsUrl(String journalUrl, int offset) throws FetcherException {
        String trimmedJournalUrl = journalUrl.endsWith("/")
                                   ? journalUrl.substring(0, journalUrl.length() - 1)
                                   : journalUrl;
        try {
            URIBuilder uriBuilder = new URIBuilder(trimmedJournalUrl + "/" + SUBMISSIONS_PATH);
            uriBuilder.addParameter("count", String.valueOf(PAGE_SIZE));
            uriBuilder.addParameter("offset", String.valueOf(offset));
            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            LOGGER.warn("Could not build OJS submissions URL for {}", journalUrl, e);
            throw new FetcherException("Malformed OJS journal URL: " + journalUrl, e);
        }
    }

    /// Derives a display name for the journal from its URL path segment, e.g.
    /// `https://example.org/index.php/myjournal` -> `myjournal`.
    /// This is a placeholder until the journal's real display name is stored alongside
    /// the URL in Preferences (see the follow-up PR for OJS journal credential storage)
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

