package org.jabref.logic.importer;

public class APIKeyPreferences {
    private final String worldcatKey;
    
    public APIKeyPreferences(String worldcatKey) {
        this.worldcatKey = worldcatKey;
    }

    public String getWorldcatKey() { return worldcatKey; }
}