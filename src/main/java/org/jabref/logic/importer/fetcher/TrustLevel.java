package org.jabref.logic.importer.fetcher;

public enum TrustLevel {
    ORIGINAL(3),
    PUBLISHER(2),
    META(1),
    UNKNOWN(0);

    private int trust;

    TrustLevel(int trust) {
        this.trust = trust;
    }

    public boolean isHigherThan(TrustLevel other) {
        return this.trust > other.trust;
    }
}
