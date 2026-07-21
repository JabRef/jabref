package org.jabref.logic.importer.fetcher;

import java.time.Duration;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class FetcherRateLimiterTest {

    @Test
    void convertsRequestsPerIntervalToRequestsPerSecond() {
        FetcherRateLimiter rateLimiter = FetcherRateLimiter.ofRequestsPerInterval("Test", 1, Duration.ofSeconds(3));

        assertEquals(1.0 / 3.0, rateLimiter.getRate());
    }

    @Test
    void updatesRate() {
        FetcherRateLimiter rateLimiter = FetcherRateLimiter.ofRequestsPerSecond("Test", 1.0);

        rateLimiter.setRate(2.0);

        assertEquals(2.0, rateLimiter.getRate());
    }
}
