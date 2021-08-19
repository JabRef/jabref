package org.jabref.logic.importer.importsettings;

public class ImportSettingsPreferences {

    private final boolean shouldGenerateNewKeyOnImport;
    private final boolean grobidEnabled;
    private final boolean grobidOptOut;
    private final String grobidURL;

    public ImportSettingsPreferences(boolean shouldGenerateNewKeyOnImport, boolean grobidEnabled, boolean grobidOptOut, String grobidURL) {
        this.shouldGenerateNewKeyOnImport = shouldGenerateNewKeyOnImport;
        this.grobidEnabled = grobidEnabled;
        this.grobidOptOut = grobidOptOut;
        this.grobidURL = grobidURL;
    }

    public boolean generateNewKeyOnImport() {
        return shouldGenerateNewKeyOnImport;
    }

    public boolean isGrobidEnabled() {
        return grobidEnabled;
    }

    public boolean isGrobidOptOut() {
        return grobidOptOut;
    }

    public String getGrobidURL() {
        return grobidURL;
    }

    public ImportSettingsPreferences withGrobidEnabled(boolean grobidEnabled) {
        return new ImportSettingsPreferences(shouldGenerateNewKeyOnImport, grobidEnabled, grobidOptOut, grobidURL);
    }

    public ImportSettingsPreferences withGrobidOptOut(boolean grobidOptOut) {
        return new ImportSettingsPreferences(shouldGenerateNewKeyOnImport, grobidEnabled, grobidOptOut, grobidURL);
    }
}
