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
            "https://api.springernature.com/meta/v1/json?q=anything, https://api.springernature.com/meta/v1/json?q=anything",
            "https://api.springernature.com/meta/v1/json, https://api.springernature.com/meta/v1/json"
    })
    void getRedactedUrl(String url, String redactedUrl) {
        assertEquals(redactedUrl, FetcherException.getRedactedUrl(url));
    }
}
