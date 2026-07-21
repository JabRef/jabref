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
        this.serviceName = serviceName;
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    static FetcherRateLimiter ofRequestsPerSecond(String serviceName, double requestsPerSecond) {
        return new FetcherRateLimiter(serviceName, requestsPerSecond);
    }

    static FetcherRateLimiter ofRequestsPerInterval(String serviceName, int requests, Duration interval) {
        double requestsPerSecond = requests * 1_000_000_000.0 / interval.toNanos();
        return ofRequestsPerSecond(serviceName, requestsPerSecond);
    }

    void acquire() {
        double waitingTime = rateLimiter.acquire();
        LOGGER.trace("Thread {} waited {} seconds because of the {} API rate limiter",
                Thread.currentThread().threadId(), waitingTime, serviceName);
    }

    double getRate() {
        return rateLimiter.getRate();
    }

    void setRate(double requestsPerSecond) {
        rateLimiter.setRate(requestsPerSecond);
    }
}
