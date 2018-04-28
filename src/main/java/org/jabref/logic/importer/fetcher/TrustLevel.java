package org.jabref.logic.importer.fetcher;

public enum TrustLevel {
    SOURCE(3),
    PUBLISHER(2),
    PREPRINT(1),
    META_SEARCH(1),
    UNKNOWN(0);

    private int score;

    TrustLevel(int score) {
        this.score = score;
    }

    public int getTrustScore() {
        return this.score;
    }
}
