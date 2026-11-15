package org.jabref.logic.net.provider;

import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jabref.logic.importer.FetcherException;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import org.jspecify.annotations.NullMarked;

/// The rules of one provider: its identification, its rate limits, its limit on parallel requests and its retries.
///
/// `URLDownload` adds the identification via [#identify(URL)] and opens every connection to the provider through [#execute(URL, Connector)].
/// The limits can depend on the requested URL, e.g. on the pool or the request type (see [CrossrefPolicy]).
@NullMarked
public final class ProviderPolicy {

    /// The limits that apply to one request.
    ///
    /// @param rateLimiter requests per period
    /// @param bulkhead    parallel requests; a permit is held while the connection is opened, i.e. until the response headers arrive
    public record Limits(RateLimiter rateLimiter, Bulkhead bulkhead) {
    }

    /// The request to send: the URL and additional request headers.
    public record Request(URL url, Map<String, String> headers) {
        public static Request of(URL url) {
            return new Request(url, Map.of());
        }
    }

    /// Looks up the configured API key or contact email of a fetcher, see `ImporterPreferences#getApiKey`.
    @FunctionalInterface
    public interface ApiKeys {
        Optional<String> get(String fetcherName);
    }

    /// Adds the identification the provider expects (API key, contact email) to a request.
    @FunctionalInterface
    public interface Identification {
        Request identify(URL url, ApiKeys apiKeys);
    }

    /// Opens the connection and checks the HTTP status. Throws [FetcherException] for HTTP errors.
    @FunctionalInterface
    public interface Connector {
        URLConnection connect() throws FetcherException;
    }

    private final String providerName;
    private final Predicate<URL> appliesTo;
    private final Function<URL, Limits> limitsForUrl;
    private final Identification identification;
    private final Retry retry;

    public ProviderPolicy(String providerName, Predicate<URL> appliesTo, Identification identification, Function<URL, Limits> limitsForUrl, Retry retry) {
        this.providerName = providerName;
        this.appliesTo = appliesTo;
        this.identification = identification;
        this.limitsForUrl = limitsForUrl;
        this.retry = retry;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean appliesTo(URL url) {
        return appliesTo.test(url);
    }

    /// Adds the provider's identification, using the API keys registered at [ProviderPolicies#useApiKeys(ApiKeys)].
    public Request identify(URL url) {
        return identification.identify(url, ProviderPolicies.apiKeys());
    }

    Limits limitsFor(URL url) {
        return limitsForUrl.apply(url);
    }

    /// Opens a connection under the provider's rules.
    ///
    /// Each attempt first waits for a parallel request slot and then for a rate permit.
    /// The retry is the outermost policy, so every retry takes a new slot and a new permit.
    /// The slot is released once the connection is open: the response body is not covered.
    public URLConnection execute(URL url, Connector connector) throws FetcherException {
        Limits limits = limitsFor(url);
        CheckedSupplier<URLConnection> attempt = Bulkhead.decorateCheckedSupplier(limits.bulkhead(), () -> {
            RateLimiter.waitForPermission(limits.rateLimiter());
            return connector.connect();
        });
        try {
            return Retry.decorateCheckedSupplier(retry, attempt).get();
        } catch (FetcherException e) {
            throw e;
        } catch (AcquirePermissionCancelledException e) {
            Thread.currentThread().interrupt();
            throw new FetcherException("Interrupted while waiting for " + providerName, e);
        } catch (RequestNotPermitted | BulkheadFullException e) {
            throw new FetcherException("Waited too long for a free request slot at " + providerName, e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable throwable) {
            throw new FetcherException("Request to " + providerName + " failed", throwable);
        }
    }
}
