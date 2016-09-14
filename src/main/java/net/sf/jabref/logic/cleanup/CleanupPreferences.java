package net.sf.jabref.logic.cleanup;

import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class CleanupPreferences {

    private final String fileNamePattern;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final FileDirectoryPreferences fileDirectoryPreferences;

    public CleanupPreferences(String fileNamePattern, LayoutFormatterPreferences layoutFormatterPreferences,
            FileDirectoryPreferences fileDirectoryPreferences) {
        this.fileNamePattern = fileNamePattern;
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return layoutFormatterPreferences;
    }

    public FileDirectoryPreferences getFileDirectoryPreferences() {
        return fileDirectoryPreferences;
    }
}
