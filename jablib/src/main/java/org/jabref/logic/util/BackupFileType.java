package org.jabref.logic.util;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum BackupFileType implements FileType {

    // Used at BackupManager
    BACKUP("Backup", "bak"),

    // Used when writing the .bib file. See {@link org.jabref.logic.exporter.AtomicFileWriter}
    // Used for copying the .bib away before overwriting on save.
    SAVE("AutoSaveFile", "sav");

    private final String name;

    private final List<String> extensions;

    BackupFileType(String name, String extension) {
        this.name = name;
        this.extensions = List.of(extension);
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
