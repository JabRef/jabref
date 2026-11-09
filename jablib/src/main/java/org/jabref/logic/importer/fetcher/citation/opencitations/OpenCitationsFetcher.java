package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherRetry;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RateLimiter;
import dev.failsafe.RetryPolicy;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class OpenCitationsFetcher implements CitationFetcher {
    public static final String FETCHER_NAME = "OpenCitations";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCitationsFetcher.class);
    private static final String API_BASE_URL = "https://api.opencitations.net/index/v2";
    private static final Gson GSON = new Gson();

    private static final RetryPolicy<String> RETRY_POLICY = RetryPolicy.<String>builder()
           .handleIf(OpenCitationsFetcher::isRetryable)
           .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(8))
           .withMaxRetries(4)
           .withMaxDuration(Duration.ofSeconds(10))
           .onRetryScheduled(event -> LOGGER.info("Received HTTP 429 from OpenCitations. Retry {} after {}", event.getAttemptCount(), event.getDelay()))
           .build();

    // Failsafe rejects a request at once if no permit is free. The long maximum makes it wait instead.
    private static final RateLimiter<String> RATE_LIMITER = RateLimiter.<String>smoothBuilder(Duration.ofSeconds(1))
           .withMaxWaitTime(Duration.ofSeconds(10))
           .onSuccess(event -> LOGGER.trace("Waited {} seconds before acquiring RateLimiter because of the {} API rate limiter", event.getElapsedTime().toSeconds(), FETCHER_NAME))
           .build();

    // The first policy is the outermost one, so each retry draws a new permit.
    private static final FailsafeExecutor<String> DOWNLOADER = Failsafe.with(RETRY_POLICY, RATE_LIMITER);

    private final ImporterPreferences importerPreferences;
    private final CrossRef crossRefFetcher;

    public OpenCitationsFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
        this.crossRefFetcher = new CrossRef(importerPreferences);
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    private Optional<URI> getApiUrl(String endpoint, BibEntry entry) {
        Optional<DOI> doi = entry.getDOI();
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        try {
            String apiUrl = API_BASE_URL + "/" + endpoint + "/doi:" + doi.get().asString();
            return Optional.of(new URI(apiUrl));
        } catch (URISyntaxException e) {
            LOGGER.debug("Could not create API URI for endpoint: {}", endpoint, e);
            return Optional.empty();
        }
    }

    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        Optional<URI> apiUri = getReferencesApiUri(entry);
        if (apiUri.isEmpty()) {
            return List.of();
        }
        return fetchCitationData(apiUri.get(), CitationItem::citedIdentifiers);
    }

    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        Optional<URI> apiUri = getCitationsApiUri(entry);
        if (apiUri.isEmpty()) {
            return List.of();
        }
        return fetchCitationData(apiUri.get(), CitationItem::citingIdentifiers);
    }

    /// API explained at <https://api.opencitations.net/index/v2#/references/{id}> and <https://api.opencitations.net/index/v2#/citations/{id}>
    private List<BibEntry> fetchCitationData(URI apiUri, Function<CitationItem, List<CitationItem.IdentifierWithField>> identifierExtractor) throws FetcherException {
        LOGGER.debug("Fetching citation data from: {}", apiUri);

        try {
            URL url = apiUri.toURL();
            URLDownload urlDownload = new URLDownload(importerPreferences, url);
            importerPreferences.getApiKey(getName())
                               .ifPresent(apiKey -> urlDownload.addHeader("authorization", apiKey));

            String jsonResponse = download(urlDownload);
            CitationItem[] citationItems = GSON.fromJson(jsonResponse, CitationItem[].class);

            if (citationItems == null || citationItems.length == 0) {
                return List.of();
            }

            List<BibEntry> entries = new ArrayList<>();
            for (CitationItem item : citationItems) {
                List<CitationItem.IdentifierWithField> identifiers = identifierExtractor.apply(item);
                if (!identifiers.isEmpty()) {
                    entries.add(fetchBibEntryFromIdentifiers(identifiers));
                }
            }

            return entries;
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        } catch (JsonSyntaxException e) {
            throw new FetcherException("Could not parse JSON response from OpenCitations", e);
        }
    }

    /// Downloads with retries; every attempt first waits for the shared rate limiter.
    private static String download(URLDownload urlDownload) throws FetcherException {
        try {
            return DOWNLOADER.get(urlDownload::asString);
        } catch (FailsafeException e) {
            // Failsafe wraps checked exceptions. The only other one here is InterruptedException, for which it has already restored the interrupt flag.
            if (e.getCause() instanceof FetcherException fetcherException) {
                throw fetcherException;
            }
            throw new FetcherException("Interrupted while waiting for OpenCitations", e);
        }
    }

    private static boolean isRetryable(Throwable throwable) {
        return throwable instanceof FetcherException fetcherException
                && fetcherException.getHttpResponse()
                                   .filter(response -> response.statusCode() == FetcherRetry.HTTP_TOO_MANY_REQUESTS)
                                   .isPresent();
    }

    private BibEntry fetchBibEntryFromIdentifiers(List<CitationItem.IdentifierWithField> identifiers) {
        Optional<CitationItem.IdentifierWithField> doiIdentifier = identifiers.stream()
                                                                              .filter(id -> id.field().equals(StandardField.DOI))
                                                                              .findFirst();

        if (doiIdentifier.isPresent()) {
            try {
                Optional<BibEntry> fetchedEntry = crossRefFetcher.performSearchById(doiIdentifier.get().value());
                if (fetchedEntry.isPresent()) {
                    BibEntry entry = fetchedEntry.get();
                    for (CitationItem.IdentifierWithField identifier : identifiers) {
                        if (!entry.hasField(identifier.field())) {
                            entry.setField(identifier.field(), identifier.value());
                        }
                    }
                    return entry;
                }
            } catch (FetcherException e) {
                LOGGER.warn("Could not fetch BibEntry for DOI: {}", doiIdentifier.get().value(), e);
            }
        }

        BibEntry bibEntry = new BibEntry();
        for (CitationItem.IdentifierWithField identifier : identifiers) {
            bibEntry.setField(identifier.field(), identifier.value());
        }
        return bibEntry;
    }

    /// API explained at <https://api.opencitations.net/index/v2#/reference-count/{id}>
    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        Optional<URI> apiUri = getApiUrl("citation-count", entry);
        if (apiUri.isEmpty()) {
            return Optional.empty();
        }

        LOGGER.debug("Citation count URL: {}", apiUri.get());

        try {
            URL url = apiUri.get().toURL();
            URLDownload urlDownload = new URLDownload(importerPreferences, url);
            importerPreferences.getApiKey(getName())
                               .ifPresent(apiKey -> urlDownload.addHeader("authorization", apiKey));

            String jsonResponse = download(urlDownload);
            CountResponse countResponse = GSON.fromJson(jsonResponse, CountResponse.class);

            if (countResponse == null) {
                return Optional.empty();
            }

            int count = countResponse.countAsInt();
            return count > 0 ? Optional.of(count) : Optional.empty();
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        } catch (JsonSyntaxException e) {
            throw new FetcherException("Could not parse JSON response from OpenCitations", e);
        }
    }

    @Override
    public Optional<URI> getReferencesApiUri(BibEntry entry) {
        return getApiUrl("references", entry);
    }

    @Override
    public Optional<URI> getCitationsApiUri(BibEntry entry) {
        return getApiUrl("citations", entry);
    }
}
