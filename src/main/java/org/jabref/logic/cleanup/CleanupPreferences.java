package org.jabref.logic.cleanup;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.metadata.FilePreferences;

public class CleanupPreferences {

    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final FilePreferences filePreferences;

    public CleanupPreferences(LayoutFormatterPreferences layoutFormatterPreferences, FilePreferences filePreferences) {
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.filePreferences = filePreferences;
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return layoutFormatterPreferences;
    }

    public FilePreferences getFilePreferences() {
        return filePreferences;
    }
}
