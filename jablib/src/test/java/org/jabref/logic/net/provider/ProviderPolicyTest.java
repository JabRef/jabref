package org.jabref.logic.net.provider;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.model.http.SimpleHttpResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProviderPolicyTest {

    private URL url;
    private Bulkhead bulkhead;
    private ProviderPolicy policy;

    @BeforeEach
    void setUp() throws Exception {
        url = URI.create("https://api.example.org/works").toURL();
        bulkhead = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());
        RateLimiter rateLimiter = RateLimiter.of("test", RateLimiterConfig.custom()
                                                                        .limitForPeriod(100)
                                                                        .limitRefreshPeriod(Duration.ofSeconds(1))
                                                                        .timeoutDuration(Duration.ZERO)
                                                                        .build());
        Retry retry = Retry.of("test", RetryConfig.custom()
                                                  .maxAttempts(3)
                                                  .retryOnException(ProviderPolicies::isTooManyRequests)
                                                  .waitDuration(Duration.ofMillis(1))
                                                  .build());
        policy = new ProviderPolicy("Test", _ -> true, (url, _) -> ProviderPolicy.Request.of(url), _ -> new ProviderPolicy.Limits(rateLimiter, bulkhead), retry);
    }

    @Test
    // [utest->req~fetchers.identifier-rate-limit-retries~1]
    void retriesTooManyRequestsUntilSuccess() throws Exception {
        URLConnection connection = mock(URLConnection.class);
        AtomicInteger attempts = new AtomicInteger();

        URLConnection result = policy.execute(url, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw tooManyRequests();
            }
            return connection;
        });

        assertSame(connection, result);
        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryOtherClientErrors() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(FetcherClientException.class, () -> policy.execute(url, () -> {
            attempts.incrementAndGet();
            throw new FetcherClientException(url, new SimpleHttpResponse(404, "Not Found", ""));
        }));
        assertEquals(1, attempts.get());
    }

    @Test
    void releasesParallelRequestPermitOnceConnected() throws Exception {
        AtomicInteger availableWhileConnecting = new AtomicInteger(-1);

        policy.execute(url, () -> {
            availableWhileConnecting.set(bulkhead.getMetrics().getAvailableConcurrentCalls());
            return mock(URLConnection.class);
        });

        assertEquals(0, availableWhileConnecting.get());
        assertEquals(1, bulkhead.getMetrics().getAvailableConcurrentCalls());
    }

    @Test
    void releasesParallelRequestPermitWhenAllAttemptsFail() {
        assertThrows(FetcherClientException.class, () -> policy.execute(url, () -> {
            throw tooManyRequests();
        }));

        assertEquals(1, bulkhead.getMetrics().getAvailableConcurrentCalls());
    }

    private FetcherClientException tooManyRequests() {
        return new FetcherClientException(url, new SimpleHttpResponse(ProviderPolicies.HTTP_TOO_MANY_REQUESTS, "Too Many Requests", ""));
    }
}
