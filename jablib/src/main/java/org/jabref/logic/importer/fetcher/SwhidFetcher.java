package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.identifier.SWHID;

import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class SwhidFetcher implements IdBasedFetcher {

    public static final String FETCHER_NAME = "Software Heritage";
    private static final Logger LOGGER = LoggerFactory.getLogger(SwhidFetcher.class);
    private static final String API_URL = "https://archive.softwareheritage.org/api/1/raw-intrinsic-metadata/citation/swhid/";

    // Software Heritage allows 120 requests per hour for anonymous users
    // [impl->req~fetchers.rate-limiting~1]
    static final FetcherRateLimiter RATE_LIMITER = FetcherRateLimiter.ofRequestsPerInterval(FETCHER_NAME, 120, Duration.ofHours(1));
    private final ImportFormatPreferences importFormatPreferences;

    public SwhidFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    public URL getURLForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        return new URIBuilder(API_URL)
                .addParameter("citation_format", "bibtex")
                .addParameter("target_swhid", identifier)
                .build()
                .toURL();
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<SWHID> parsedSwhid = SWHID.parse(identifier);
        if (parsedSwhid.isEmpty()) {
            throw new FetcherException("Invalid SWHID format: " + identifier);
        }

        String canonicalSwhid = parsedSwhid.get().asString();
        RATE_LIMITER.acquire(canonicalSwhid);

        URL url;
        try {
            url = getURLForIdentifier(canonicalSwhid);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Invalid URL constructed for SWHID: " + canonicalSwhid, e);
        }

        try {
            URLDownload urlDownload = new URLDownload(url);
            urlDownload.addHeader("User-Agent", "JabRef");
            String response = urlDownload.asString();

            JSONObject jsonObject = new JSONObject(response);
            String bibtexContext = jsonObject.optString("content", "");

            if (bibtexContext.isBlank()) {
                return Optional.empty();
            }

            BibtexParser parser = new BibtexParser(importFormatPreferences);
            List<BibEntry> entries = parser.parseEntries(bibtexContext);

            if (entries.isEmpty()) {
                return Optional.empty();
            }

            BibEntry entry = entries.getFirst();

            if (!entry.hasField(BiblatexSoftwareField.SWHID)) {
                entry = entry.withField(BiblatexSoftwareField.SWHID, canonicalSwhid);
            }

            return Optional.of(entry);
        } catch (FetcherClientException e) {
            boolean isNotFound = e.getHttpResponse()
                                  .map(response -> response.statusCode() == 404)
                                  .orElse(false);
            if (isNotFound) {
                LOGGER.debug("No citation metadata found for SWHID: {}", canonicalSwhid, e);
                return Optional.empty();
            }
            throw e;
        } catch (JSONException | ParseException e) {
            LOGGER.info("Error fetching or parsing SWHID response for {}", canonicalSwhid, e);
            throw new FetcherException("Failed to retrieve or parse metadata from Software Heritage", e);
        }
    }
}
