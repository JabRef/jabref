package org.jabref.logic.importer;

public class FetcherApiPreferences {
    private final String worldcatKey;

    public FetcherApiPreferences(String worldcatKey) {
        this.worldcatKey = worldcatKey;
    }

    public String getWorldcatKey() {
        return worldcatKey;
    }
}
