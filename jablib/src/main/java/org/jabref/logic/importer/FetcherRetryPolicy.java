package org.jabref.logic.importer;

import java.time.Duration;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FetcherRetryPolicy(
        int maxRetries,
        Set<Integer> retryableStatusCodes,
        Duration initialBackoff,
        Duration maxTotalWait) {

    /// avoid overflow by capping to 2^30
    static final int MAX_RETRIES_LIMIT = 30;

    public FetcherRetryPolicy {
        if ((maxRetries < 0) || (maxRetries > MAX_RETRIES_LIMIT)) {
            throw new IllegalArgumentException("maxRetries must be between 0 and " + MAX_RETRIES_LIMIT);
        }
        if (initialBackoff.isNegative() || maxTotalWait.isNegative()) {
            throw new IllegalArgumentException("Durations must not be negative");
        }
        retryableStatusCodes = Set.copyOf(retryableStatusCodes);
    }

    public boolean isRetryable(int statusCode) {
        return retryableStatusCodes.contains(statusCode);
    }

    /// Computes the delay before a retry: `initialBackoff * 2^retryAttempt`. (incremental backoff)
    ///
    /// @param retryAttempt zero-based number of the retry (0 = first retry)
    public Duration delayFor(int retryAttempt) {
        return initialBackoff.multipliedBy(1L << retryAttempt);
    }
}
