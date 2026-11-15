package org.jabref.logic.net.provider;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.regex.Pattern;

import org.jabref.logic.importer.fetcher.CrossRef;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;

/// Rules of the Crossref REST API (`api.crossref.org`).
///
/// Crossref has two pools and two request types, each with its own limits
/// ([announcement](https://www.crossref.org/blog/announcing-changes-to-rest-api-rate-limits/)):
///
/// | Pool                      | Request type  | Requests per second | Parallel requests |
/// |---------------------------|---------------|---------------------|-------------------|
/// | public                    | list          | 1                   | 1                 |
/// | public                    | single record | 5                   | 1                 |
/// | polite (`mailto` present) | list          | 3                   | 3                 |
/// | polite                    | single record | 10                  | 3                 |
///
/// The policy adds the configured contact email as `mailto` parameter, so every request to Crossref uses the polite pool,
/// whichever fetcher sends it. The pool is then read from the URL that is actually sent, so the limit always matches it.
@NullMarked
// [impl->req~fetchers.rate-limiting~1]
final class CrossrefPolicy {

    static final String HOST = "api.crossref.org";

    /// A single record is addressed by one path segment after the resource, e.g. `/works/{doi}` or `/journals/{issn}`.
    /// Everything else (`/works?query=…`, `/members/{id}/works`) is a list.
    private static final Pattern SINGLE_RECORD_PATH = Pattern.compile("^/(?:v1/)?[a-z]+/[^/]+$");
    private static final Pattern MAILTO_PARAMETER = Pattern.compile("(?:^|&)mailto=[^&]+");

    private static final String MAILTO = "mailto";

    private static final Duration MAX_WAIT = Duration.ofSeconds(30);

    private static final Bulkhead PUBLIC_PARALLEL = bulkhead("crossref-public", 1);
    private static final Bulkhead POLITE_PARALLEL = bulkhead("crossref-polite", 3);

    private static final ProviderPolicy.Limits PUBLIC_LIST = new ProviderPolicy.Limits(rateLimiter("crossref-public-list", 1), PUBLIC_PARALLEL);
    private static final ProviderPolicy.Limits PUBLIC_SINGLE = new ProviderPolicy.Limits(rateLimiter("crossref-public-single", 5), PUBLIC_PARALLEL);
    private static final ProviderPolicy.Limits POLITE_LIST = new ProviderPolicy.Limits(rateLimiter("crossref-polite-list", 3), POLITE_PARALLEL);
    private static final ProviderPolicy.Limits POLITE_SINGLE = new ProviderPolicy.Limits(rateLimiter("crossref-polite-single", 10), POLITE_PARALLEL);

    private CrossrefPolicy() {
    }

    static ProviderPolicy create(Retry retry) {
        return new ProviderPolicy("Crossref", url -> HOST.equalsIgnoreCase(url.getHost()), CrossrefPolicy::identify, CrossrefPolicy::limitsFor, retry);
    }

    /// Adds the configured contact email unless the URL already carries one.
    // [impl->req~fetchers.crossref-polite-pool~1]
    static ProviderPolicy.Request identify(URL url, ProviderPolicy.ApiKeys apiKeys) {
        if (hasMailto(url)) {
            return ProviderPolicy.Request.of(url);
        }
        return apiKeys.get(CrossRef.FETCHER_NAME)
                      .map(email -> ProviderPolicy.Request.of(withParameter(url, MAILTO, email)))
                      .orElseGet(() -> ProviderPolicy.Request.of(url));
    }

    static ProviderPolicy.Limits limitsFor(URL url) {
        boolean polite = hasMailto(url);
        boolean singleRecord = SINGLE_RECORD_PATH.matcher(url.getPath()).matches();
        if (polite) {
            return singleRecord ? POLITE_SINGLE : POLITE_LIST;
        }
        return singleRecord ? PUBLIC_SINGLE : PUBLIC_LIST;
    }

    private static boolean hasMailto(URL url) {
        return url.getQuery() != null && MAILTO_PARAMETER.matcher(url.getQuery()).find();
    }

    private static URL withParameter(URL url, String name, String value) {
        try {
            return new URIBuilder(url.toURI()).addParameter(name, value).build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Cannot add a parameter to " + url, e);
        }
    }

    private static RateLimiter rateLimiter(String name, int requestsPerSecond) {
        return RateLimiter.of(name, RateLimiterConfig.custom()
                                                    .limitForPeriod(requestsPerSecond)
                                                    .limitRefreshPeriod(Duration.ofSeconds(1))
                                                    .timeoutDuration(MAX_WAIT)
                                                    .build());
    }

    private static Bulkhead bulkhead(String name, int parallelRequests) {
        return Bulkhead.of(name, BulkheadConfig.custom()
                                               .maxConcurrentCalls(parallelRequests)
                                               .maxWaitDuration(MAX_WAIT)
                                               .build());
    }
}
