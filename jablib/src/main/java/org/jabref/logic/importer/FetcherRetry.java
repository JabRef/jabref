package org.jabref.logic.importer;

import java.time.Duration;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
final class FetcherRetry {

    static final int MAX_RATE_LIMIT_RETRIES = 2;
    static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static final Logger LOGGER = LoggerFactory.getLogger(FetcherRetry.class);
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    private FetcherRetry() {
    }

    // [impl->req~fetchers.identifier-rate-limit-retries~1]
    static <T> T executeWithRateLimitRetry(FetcherOperation<T> operation) throws FetcherException {
        return executeWithRateLimitRetry(operation, Thread::sleep);
    }

    static <T> T executeWithRateLimitRetry(FetcherOperation<T> operation, Backoff backoff) throws FetcherException {
        for (int retryAttempt = 0; ; retryAttempt++) {
            try {
                return operation.execute();
            } catch (FetcherClientException exception) {
                if (!isRateLimited(exception) || retryAttempt == MAX_RATE_LIMIT_RETRIES) {
                    throw exception;
                }

                Duration delay = INITIAL_BACKOFF.multipliedBy(1L << retryAttempt);
                LOGGER.info("Received HTTP 429. Retrying after {}", delay);
                try {
                    backoff.waitFor(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new FetcherException("Interrupted while waiting to retry a rate-limited request", interruptedException);
                }
            }
        }
    }

    private static boolean isRateLimited(FetcherClientException exception) {
        return exception.getHttpResponse()
                        .map(response -> response.statusCode() == HTTP_TOO_MANY_REQUESTS)
                        .orElse(false);
    }

    @FunctionalInterface
    interface FetcherOperation<T> {
        T execute() throws FetcherException;
    }

    @FunctionalInterface
    interface Backoff {
        void waitFor(Duration delay) throws InterruptedException;
    }
}
