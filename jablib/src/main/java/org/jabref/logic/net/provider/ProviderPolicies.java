package org.jabref.logic.net.provider;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.jspecify.annotations.NullMarked;

/// Registry of all provider policies. `URLDownload` looks up the policy for its URL here.
///
/// A URL without a policy is downloaded as before: no rate limit, no parallel request limit, no retry.
@NullMarked
public final class ProviderPolicies {

    static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static final List<ProviderPolicy> POLICIES = List.of(
            CrossrefPolicy.create(retryOnTooManyRequests("crossref", 3, Duration.ofSeconds(1))));

    /// Until the application registers the user's preferences, the default keys of the build are used.
    private static volatile ProviderPolicy.ApiKeys apiKeys = ImporterPreferences.getDefault()::getApiKey;

    private ProviderPolicies() {
    }

    /// Registers where the policies look up API keys and contact emails. Called once at startup, like `ProxyRegisterer`.
    /// The preferences object is live, so later changes to the keys apply without registering again.
    public static void useApiKeys(ProviderPolicy.ApiKeys keys) {
        apiKeys = keys;
    }

    static ProviderPolicy.ApiKeys apiKeys() {
        return apiKeys;
    }

    public static Optional<ProviderPolicy> forUrl(URL url) {
        return POLICIES.stream().filter(policy -> policy.appliesTo(url)).findFirst();
    }

    /// Retries HTTP 429 with exponential backoff (`initialBackoff`, doubled per retry).
    // [impl->req~fetchers.identifier-rate-limit-retries~1]
    static Retry retryOnTooManyRequests(String name, int maxAttempts, Duration initialBackoff) {
        return Retry.of(name, RetryConfig.custom()
                                         .maxAttempts(maxAttempts)
                                         .retryOnException(ProviderPolicies::isTooManyRequests)
                                         .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoff, 2))
                                         .build());
    }

    static boolean isTooManyRequests(Throwable throwable) {
        return throwable instanceof FetcherException fetcherException
                && fetcherException.getHttpResponse()
                                   .filter(response -> response.statusCode() == HTTP_TOO_MANY_REQUESTS)
                                   .isPresent();
    }
}
