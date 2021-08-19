package org.jabref.logic.importer.importsettings;

public class ImportSettingsPreferences {

    private final boolean shouldGenerateNewKeyOnImport;
    private final boolean grobidEnabled;
    private final String grobidURL;

    public ImportSettingsPreferences(boolean shouldGenerateNewKeyOnImport, boolean grobidEnabled, String grobidURL) {
        this.shouldGenerateNewKeyOnImport = shouldGenerateNewKeyOnImport;
        this.grobidEnabled = grobidEnabled;
        this.grobidURL = grobidURL;
    }

    public boolean generateNewKeyOnImport() {
        return shouldGenerateNewKeyOnImport;
    }

    public boolean isGrobidEnabled() {
        return grobidEnabled;
    }

    public String getGrobidURL() {
        return grobidURL;
    }
}
