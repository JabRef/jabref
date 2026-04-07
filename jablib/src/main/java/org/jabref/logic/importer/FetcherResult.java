package org.jabref.logic.importer;

import java.net.URL;
import java.util.Map;

import org.jabref.logic.importer.fetcher.TrustLevel;

public record FetcherResult(TrustLevel trust, URL source, Map<String, String> headers) {
    public FetcherResult(TrustLevel trust, URL source) {
        this(trust, source, Map.of());
    }

    public FetcherResult(TrustLevel trust, URL source, Map<String, String> headers) {
        this.trust = trust;
        this.source = source;
        this.headers = Map.copyOf(headers);
    }
}
