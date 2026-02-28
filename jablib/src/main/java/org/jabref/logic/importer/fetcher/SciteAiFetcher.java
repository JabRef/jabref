package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.sciteTallies.TalliesResponse;

import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetches citation information from <https://scite.ai/>
public class SciteAiFetcher implements CitationCountFetcher {
    public static final String FETCHER_NAME = "scite.ai";
    private static final String BASE_URL = "https://api.scite.ai/";
    private static final Logger LOGGER = LoggerFactory.getLogger(SciteAiFetcher.class);

    public SciteAiFetcher() {
    }

    public TalliesResponse fetchTallies(DOI doi) throws FetcherException {
        URL url;
        try {
            url = new URI(BASE_URL + "tallies/" + doi.asString()).toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new FetcherException("Malformed URL for DOI", ex);
        }
        LOGGER.debug("Fetching tallies from {}", url);
        URLDownload download = new URLDownload(url);
        String response = download.asString();
        LOGGER.debug("Response {}", response);
        JSONObject tallies = new JSONObject(response);
        if (tallies.has("detail")) {
            String message = tallies.getString("detail");
            throw new FetcherException(message);
        } else if (!tallies.has("total")) {
            throw new FetcherException("Unexpected result data.");
        }
        return TalliesResponse.fromJSONObject(tallies);
    }

    @Override
    public @NonNull Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        Optional<DOI> doi = entry.getDOI();
        if (doi.isEmpty()) {
            return Optional.empty();
        }
        int total = fetchTallies(doi.get()).total();
        return Optional.of(total);
    }
}
