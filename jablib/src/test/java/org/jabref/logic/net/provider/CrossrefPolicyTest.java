package org.jabref.logic.net.provider;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrossrefPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "https://api.crossref.org/works?query=test, crossref-public-list, crossref-public",
            "https://api.crossref.org/works?query=test&mailto=user%40example.org, crossref-polite-list, crossref-polite",
            "https://api.crossref.org/works/10.1000%2F1, crossref-public-single, crossref-public",
            "https://api.crossref.org/works/10.1000%2F1?mailto=user%40example.org, crossref-polite-single, crossref-polite",
            "https://api.crossref.org/members/13/works?mailto=user%40example.org, crossref-polite-list, crossref-polite",
            "https://api.crossref.org/journals/0266-612X, crossref-public-single, crossref-public"
    })
    // [utest->req~fetchers.rate-limiting~1]
    void limitsFollowPoolAndRequestType(String url, String expectedRateLimiter, String expectedBulkhead) throws Exception {
        ProviderPolicy.Limits limits = CrossrefPolicy.limitsFor(URI.create(url).toURL());

        assertEquals(expectedRateLimiter, limits.rateLimiter().getName());
        assertEquals(expectedBulkhead, limits.bulkhead().getName());
    }

    @ParameterizedTest
    @CsvSource({
            "https://api.crossref.org/works?query=test, true",
            "https://API.CROSSREF.ORG/works, true",
            "https://doi.org/10.1000/1, false",
            "https://data.crossref.org/10.1000/1, false"
    })
    void appliesOnlyToTheRestApiHost(String url, boolean expected) throws Exception {
        URL parsed = URI.create(url).toURL();

        assertEquals(expected, ProviderPolicies.forUrl(parsed).isPresent());
    }

    @ParameterizedTest
    @CsvSource({
            "https://api.crossref.org/works?query=test, user@example.org, https://api.crossref.org/works?query=test&mailto=user%40example.org",
            "https://api.crossref.org/works/10.1000%2F1, user@example.org, https://api.crossref.org/works/10.1000%2F1?mailto=user%40example.org",
            "https://api.crossref.org/works?query=test&mailto=other%40example.org, user@example.org, https://api.crossref.org/works?query=test&mailto=other%40example.org",
            "https://api.crossref.org/works?query=test, '', https://api.crossref.org/works?query=test"
    })
    // [utest->req~fetchers.crossref-polite-pool~1]
    void addsConfiguredEmailUnlessPresent(String url, String email, String expected) throws Exception {
        ProviderPolicy.ApiKeys apiKeys = _ -> Optional.of(email).filter(value -> !value.isEmpty());

        ProviderPolicy.Request request = CrossrefPolicy.identify(URI.create(url).toURL(), apiKeys);

        assertEquals(expected, request.url().toString());
    }
}
