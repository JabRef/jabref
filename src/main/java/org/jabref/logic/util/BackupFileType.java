package org.jabref.logic.util;

import java.util.Collections;
import java.util.List;

public enum BackupFileType implements FileType {

    // Used at BackupManager
    BACKUP("Backup", "bak"),

    // Used when writing the .bib file. See {@link org.jabref.logic.exporter.AtomicFileWriter}
    // Used for copying the .bib away before overwriting on save.
    SAVE("AutoSaveFile", "sav");

    private final List<String> extensions;
    private final String name;

    BackupFileType(String name, String extension) {
        this.extensions = Collections.singletonList(extension);
        this.name = name;
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
