package org.jabref.logic.cleanup;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class CleanupPreferences {

    private final String fileNamePattern;
    private final String fileDirPattern;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final FileDirectoryPreferences fileDirectoryPreferences;


    public CleanupPreferences(String fileNamePattern, String fileDirPattern,
            LayoutFormatterPreferences layoutFormatterPreferences, FileDirectoryPreferences fileDirectoryPreferences) {
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getFileDirPattern() {
        return fileDirPattern;
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return layoutFormatterPreferences;
    }

    public FileDirectoryPreferences getFileDirectoryPreferences() {
        return fileDirectoryPreferences;
    }
}
