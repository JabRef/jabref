package org.jabref.logic.importer;

import java.net.URL;

import org.jabref.logic.importer.fetcher.TrustLevel;

public final class FetcherResult {
    public final TrustLevel trust;
    public final URL source;

    public FetcherResult(TrustLevel trust, URL source) {
        this.trust = trust;
        this.source = source;
    }
}
