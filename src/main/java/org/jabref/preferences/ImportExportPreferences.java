package org.jabref.preferences;

public class ImportExportPreferences {
    private final String nonWrappableFields;
    private final boolean resolveStringsForStandardBibtexFields;
    private final boolean resolveStringsForAllStrings;
    private final String nonResolvableFields;
    private final NewLineSeparator newLineSeparator;
    private final boolean alwaysReformatOnSave;

    public ImportExportPreferences(String nonWrappableFields,
                                   boolean resolveStringsForStandardBibtexFields,
                                   boolean resolveStringsForAllStrings,
                                   String nonResolvableFields,
                                   NewLineSeparator newLineSeparator,
                                   boolean alwaysReformatOnSave) {
        this.nonWrappableFields = nonWrappableFields;
        this.resolveStringsForStandardBibtexFields = resolveStringsForStandardBibtexFields;
        this.resolveStringsForAllStrings = resolveStringsForAllStrings;
        this.nonResolvableFields = nonResolvableFields;
        this.newLineSeparator = newLineSeparator;
        this.alwaysReformatOnSave = alwaysReformatOnSave;
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

    public NewLineSeparator getNewLineSeparator() {
        return newLineSeparator;
    }

    public boolean shouldAlwaysReformatOnSave() {
        return alwaysReformatOnSave;
    }
}
