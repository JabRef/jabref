package org.jabref.logic.importer.fetcher;

/**
 * Discussion on the trust levels is available at our <a href="https://devdocs.jabref.org/code-howtos/fetchers.html#fulltext-fetchers">documentation on fulltext fetchers</a>.
 */
public enum TrustLevel {
    SOURCE(3),
    PUBLISHER(2),
    PREPRINT(1),
    META_SEARCH(1),
    UNKNOWN(0);

    private final int score;

    TrustLevel(int score) {
        this.score = score;
    }

    public int getTrustScore() {
        return this.score;
    }
}
