package org.jabref.logic.importer;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

import org.jabref.model.http.SimpleHttpResponse;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class FetcherRetry {

    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    static final int MAX_RATE_LIMIT_RETRIES = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(FetcherRetry.class);
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    /// Default policy: two retries on HTTP 429. Capped to 10s when also waiting in the operation e.g. to acquire locks.
    static final FetcherRetryPolicy DEFAULT_POLICY = new FetcherRetryPolicy(
            MAX_RATE_LIMIT_RETRIES,
            Set.of(HTTP_TOO_MANY_REQUESTS),
            INITIAL_BACKOFF,
            Duration.ofSeconds(10));

    private FetcherRetry() {
    }

    // [impl->req~fetchers.identifier-rate-limit-retries~1]
    static <T> T executeWithRateLimitRetry(FetcherOperation<T> operation) throws FetcherException {
        return executeWithRateLimitRetry(operation, Thread::sleep);
    }

    static <T> T executeWithRateLimitRetry(FetcherOperation<T> operation, Backoff backoff) throws FetcherException {
        return execute(DEFAULT_POLICY, operation, backoff, System::nanoTime);
    }

    public static <T> T executeWithRetryPolicy(FetcherRetryPolicy policy, FetcherOperation<T> operation) throws FetcherException {
        return execute(policy, operation, Thread::sleep, System::nanoTime);
    }

    /// The clock is a parameter so that tests can simulate time spent inside the operation.
    static <T> T execute(FetcherRetryPolicy policy, FetcherOperation<T> operation, Backoff backoff, LongSupplier nanoClock) throws FetcherException {
        long start = nanoClock.getAsLong();
        for (int retryAttempt = 0; ; retryAttempt++) {
            try {
                return operation.execute();
            } catch (FetcherException exception) {
                Optional<Integer> statusCode = exception.getHttpResponse().map(SimpleHttpResponse::statusCode);
                if (statusCode.isEmpty() || !policy.isRetryable(statusCode.get()) || retryAttempt == policy.maxRetries()) {
                    throw exception;
                }

                Duration delay = policy.delayFor(retryAttempt);
                Duration elapsed = Duration.ofNanos(nanoClock.getAsLong() - start);
                if (elapsed.plus(delay).compareTo(policy.maxTotalWait()) > 0) {
                    // Give up if the elapsed time plus full delay does not fit the budget
                    throw exception;
                }

                LOGGER.info("Received HTTP {}. Retry {} of {} after {}", statusCode.get(), retryAttempt + 1, policy.maxRetries(), delay);
                try {
                    backoff.waitFor(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new FetcherException("Interrupted while waiting to retry a rate-limited request", interruptedException);
                }
            }
        }
    }

    @FunctionalInterface
    public interface FetcherOperation<T> {
        T execute() throws FetcherException;
    }

    @FunctionalInterface
    interface Backoff {
        void waitFor(Duration delay) throws InterruptedException;
    }
}
