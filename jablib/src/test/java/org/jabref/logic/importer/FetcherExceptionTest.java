package org.jabref.logic.importer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FetcherExceptionTest {

    @ParameterizedTest
    @CsvSource({
            "https://api.springernature.com/meta/v1/json?q=anything&api_key=abc&s=1&p=20, https://api.springernature.com/meta/v1/json?q=anything&api_key=[REDACTED]&s=1&p=20",
            "https://api.springernature.com/meta/v1/json?q=anything&API_KEY=abc, https://api.springernature.com/meta/v1/json?q=anything&API_KEY=[REDACTED]",
            "https://api.springernature.com/meta/v1/json?q=anything&apikey=abc123ABC, https://api.springernature.com/meta/v1/json?q=anything&apikey=[REDACTED]",
            "https://api.crossref.org/works?query=example&mailto=user%40example.org, https://api.crossref.org/works?query=example&mailto=[REDACTED]",
            "https://api.unpaywall.org/v2/10.1000/example?email=user%40example.org, https://api.unpaywall.org/v2/10.1000/example?email=[REDACTED]",
            "https://api.springernature.com/meta/v1/json?q=anything, https://api.springernature.com/meta/v1/json?q=anything",
            "https://api.springernature.com/meta/v1/json, https://api.springernature.com/meta/v1/json",
            "https://user:pass@example.com/references.bib, https://example.com/references.bib"
    })
    void getRedactedUrl(String url, String redactedUrl) {
        assertEquals(redactedUrl, FetcherException.getRedactedUrl(url));
    }
}
