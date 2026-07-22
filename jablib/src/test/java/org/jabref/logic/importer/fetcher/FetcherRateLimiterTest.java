package org.jabref.logic.importer.fetcher;

import java.time.Duration;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void rejectsNonPositiveRequestCount() {
        assertThrows(IllegalArgumentException.class,
                () -> FetcherRateLimiter.ofRequestsPerInterval("Test", 0, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> FetcherRateLimiter.ofRequestsPerInterval("Test", 1, Duration.ZERO));
    }

    @Test
    void rejectsNonFiniteRate() {
        assertThrows(IllegalArgumentException.class,
                () -> FetcherRateLimiter.ofRequestsPerSecond("Test", Double.POSITIVE_INFINITY));
    }
}
