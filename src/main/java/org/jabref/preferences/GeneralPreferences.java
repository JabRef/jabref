package org.jabref.preferences;

import java.nio.charset.Charset;

import org.jabref.model.database.BibDatabaseMode;

public class GeneralPreferences {
    private final Charset defaultEncoding;
    private final BibDatabaseMode defaultBibDatabaseMode;

    private final boolean warnAboutDuplicatesInInspection;
    private final boolean confirmDelete;
    private final boolean allowIntegerEditionBibtex;
    private final boolean memoryStickMode;
    private final boolean collectTelemetry;
    private final boolean showAdvancedHints;

    public GeneralPreferences(Charset defaultEncoding,
                              BibDatabaseMode defaultBibDatabaseMode,
                              boolean warnAboutDuplicatesInInspection,
                              boolean confirmDelete,
                              boolean allowIntegerEditionBibtex,
                              boolean memoryStickMode,
                              boolean collectTelemetry,
                              boolean showAdvancedHints) {
        this.defaultEncoding = defaultEncoding;
        this.defaultBibDatabaseMode = defaultBibDatabaseMode;
        this.warnAboutDuplicatesInInspection = warnAboutDuplicatesInInspection;
        this.confirmDelete = confirmDelete;
        this.allowIntegerEditionBibtex = allowIntegerEditionBibtex;
        this.memoryStickMode = memoryStickMode;
        this.collectTelemetry = collectTelemetry;
        this.showAdvancedHints = showAdvancedHints;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }

    public BibDatabaseMode getDefaultBibDatabaseMode() {
        return defaultBibDatabaseMode;
    }

    public boolean isWarnAboutDuplicatesInInspection() {
        return warnAboutDuplicatesInInspection;
    }

    public boolean isConfirmDelete() {
        return confirmDelete;
    }

    public boolean isAllowIntegerEditionBibtex() {
        return allowIntegerEditionBibtex;
    }

    public boolean isMemoryStickMode() {
        return memoryStickMode;
    }

    public boolean isCollectTelemetry() {
        return collectTelemetry;
    }

    public boolean shouldShowAdvancedHints() {
        return showAdvancedHints;
    }
}
