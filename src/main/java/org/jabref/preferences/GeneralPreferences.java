package org.jabref.preferences;

import java.nio.charset.Charset;

import org.jabref.model.database.BibDatabaseMode;

public class GeneralPreferences {
    private final Charset defaultEncoding;
    private final BibDatabaseMode defaultBibDatabaseMode;
    private final boolean warnAboutDuplicatesInInspection;
    private boolean confirmDelete;

    private final boolean memoryStickMode;
    private final boolean showAdvancedHints;

    public GeneralPreferences(Charset defaultEncoding,
                              BibDatabaseMode defaultBibDatabaseMode,
                              boolean warnAboutDuplicatesInInspection,
                              boolean confirmDelete,
                              boolean memoryStickMode,
                              boolean showAdvancedHints) {
        this.defaultEncoding = defaultEncoding;
        this.defaultBibDatabaseMode = defaultBibDatabaseMode;
        this.warnAboutDuplicatesInInspection = warnAboutDuplicatesInInspection;
        this.confirmDelete = confirmDelete;

        this.memoryStickMode = memoryStickMode;
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

    public boolean shouldConfirmDelete() {
        return confirmDelete;
    }

    public GeneralPreferences withConfirmDelete(boolean confirmDelete) {
        this.confirmDelete = confirmDelete;
        return this;
    }

    public boolean isMemoryStickMode() {
        return memoryStickMode;
    }

    public boolean shouldShowAdvancedHints() {
        return showAdvancedHints;
    }
}
