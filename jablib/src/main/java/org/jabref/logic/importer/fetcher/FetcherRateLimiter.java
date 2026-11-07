package org.jabref.logic.importer.fetcher;

import java.time.Duration;

import com.google.common.util.concurrent.RateLimiter;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
// [impl->req~fetchers.rate-limiting~1]
final class FetcherRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetcherRateLimiter.class);

    private final String serviceName;
    private final RateLimiter rateLimiter;

    private FetcherRateLimiter(String serviceName, double requestsPerSecond) {
        if (!Double.isFinite(requestsPerSecond) || requestsPerSecond <= 0) {
            throw new IllegalArgumentException("The request rate for %s must be finite and positive: %s"
                    .formatted(serviceName, requestsPerSecond));
        }
        this.serviceName = serviceName;
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    static FetcherRateLimiter ofRequestsPerSecond(String serviceName, double requestsPerSecond) {
        return new FetcherRateLimiter(serviceName, requestsPerSecond);
    }

    static FetcherRateLimiter ofRequestsPerInterval(String serviceName, int requests, Duration interval) {
        if (requests <= 0 || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("The request limit for %s must use a positive request count and interval: requests=%s, interval=%s"
                    .formatted(serviceName, requests, interval));
        }
        double requestsPerSecond = requests * 1_000_000_000.0 / interval.toNanos();
        return ofRequestsPerSecond(serviceName, requestsPerSecond);
    }

    void acquire(String requestContext) {
        double waitingTime = rateLimiter.acquire();
        LOGGER.trace("Thread {} waited {} seconds before requesting '{}' because of the {} API rate limiter",
                Thread.currentThread().threadId(), waitingTime, requestContext, serviceName);
    }

    double getRate() {
        return rateLimiter.getRate();
    }

    void setRate(double requestsPerSecond) {
        rateLimiter.setRate(requestsPerSecond);
    }
}
