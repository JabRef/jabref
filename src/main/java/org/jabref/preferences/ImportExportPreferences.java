package org.jabref.preferences;

import java.nio.file.Path;

public class ImportExportPreferences {
    private final String nonWrappableFields;
    private final boolean resolveStringsForStandardBibtexFields;
    private final boolean resolveStringsForAllStrings;
    private final String nonResolvableFields;
    private final boolean alwaysReformatOnSave;
    private Path importWorkingDirectory;
    private String lastExportExtension;
    private Path exportWorkingDirectory;

    public ImportExportPreferences(String nonWrappableFields,
                                   boolean resolveStringsForStandardBibtexFields,
                                   boolean resolveStringsForAllStrings,
                                   String nonResolvableFields,
                                   boolean alwaysReformatOnSave,
                                   Path importWorkingDirectory,
                                   String lastExportExtension,
                                   Path exportWorkingDirectory) {
        this.nonWrappableFields = nonWrappableFields;
        this.resolveStringsForStandardBibtexFields = resolveStringsForStandardBibtexFields;
        this.resolveStringsForAllStrings = resolveStringsForAllStrings;
        this.nonResolvableFields = nonResolvableFields;
        this.alwaysReformatOnSave = alwaysReformatOnSave;
        this.importWorkingDirectory = importWorkingDirectory;
        this.lastExportExtension = lastExportExtension;
        this.exportWorkingDirectory = exportWorkingDirectory;
    }

    public String getNonWrappableFields() {
        return nonWrappableFields;
    }

    public boolean shouldResolveStringsForStandardBibtexFields() {
        return resolveStringsForStandardBibtexFields;
    }

    public boolean shouldResolveStringsForAllStrings() {
        return resolveStringsForAllStrings;
    }

    public String getNonResolvableFields() {
        return nonResolvableFields;
    }

    public boolean shouldAlwaysReformatOnSave() {
        return alwaysReformatOnSave;
    }

    public Path getImportWorkingDirectory() {
        return importWorkingDirectory;
    }

    public ImportExportPreferences withImportWorkingDirectory(Path importWorkingDirectory) {
        this.importWorkingDirectory = importWorkingDirectory;
        return this;
    }

    public String getLastExportExtension() {
        return lastExportExtension;
    }

    public ImportExportPreferences withLastExportExtension(String lastExportExtension) {
        this.lastExportExtension = lastExportExtension;
        return this;
    }

    public Path getExportWorkingDirectory() {
        return exportWorkingDirectory;
    }

    public ImportExportPreferences withExportWorkingDirectory(Path exportWorkingDirectory) {
        this.exportWorkingDirectory = exportWorkingDirectory;
        return this;
    }
}
