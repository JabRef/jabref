package org.jabref.logic.importer.importsettings;

public class ImportSettingsPreferences {

    private final boolean shouldGenerateNewKeyOnImport;

    public ImportSettingsPreferences(boolean shouldGenerateNewKeyOnImport) {
        this.shouldGenerateNewKeyOnImport = shouldGenerateNewKeyOnImport;
    }

    public boolean generateNewKeyOnImport() {
        return shouldGenerateNewKeyOnImport;
    }

}
