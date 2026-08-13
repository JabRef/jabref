package org.jabref.logic.importer;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.util.URLUtil;
import org.jabref.model.http.SimpleHttpResponse;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class FetcherRetryTest {

    @Test
    void retriesRateLimitedOperation() throws FetcherException, MalformedURLException {
        AtomicInteger attempts = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        FetcherClientException rateLimited = fetcherClientException(FetcherRetry.HTTP_TOO_MANY_REQUESTS);

        String result = FetcherRetry.executeWithRateLimitRetry(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw rateLimited;
            }
            return "success";
        }, delays::add);

        assertEquals("success", result);
        assertEquals(2, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1)), delays);
    }

    @Test
    void doesNotRetryOtherClientErrors() throws MalformedURLException {
        AtomicInteger attempts = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        FetcherClientException badRequest = fetcherClientException(HttpURLConnection.HTTP_BAD_REQUEST);

        assertThrows(FetcherClientException.class, () -> FetcherRetry.executeWithRateLimitRetry(() -> {
            attempts.incrementAndGet();
            throw badRequest;
        }, delays::add));

        assertEquals(1, attempts.get());
        assertEquals(List.of(), delays);
    }

    @Test
    void stopsAfterMaximumRateLimitRetries() throws MalformedURLException {
        AtomicInteger attempts = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        FetcherClientException rateLimited = fetcherClientException(FetcherRetry.HTTP_TOO_MANY_REQUESTS);

        assertThrows(FetcherClientException.class, () -> FetcherRetry.executeWithRateLimitRetry(() -> {
            attempts.incrementAndGet();
            throw rateLimited;
        }, delays::add));

        assertEquals(FetcherRetry.MAX_RATE_LIMIT_RETRIES + 1, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), delays);
    }

    @Test
    void restoresInterruptStatusWhenBackoffIsInterrupted() throws MalformedURLException {
        FetcherClientException rateLimited = fetcherClientException(FetcherRetry.HTTP_TOO_MANY_REQUESTS);
        try {
            assertThrows(FetcherException.class, () -> FetcherRetry.executeWithRateLimitRetry(
                    () -> {
                        throw rateLimited;
                    },
                    delay -> {
                        throw new InterruptedException();
                    }));

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private FetcherClientException fetcherClientException(int statusCode) throws MalformedURLException {
        return new FetcherClientException(
                URLUtil.create("https://example.org"),
                new SimpleHttpResponse(statusCode, "Error", ""));
    }
}
