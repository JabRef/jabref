package org.jabref.logic.util;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum BackupFileType implements FileType {

    // Used at BackupManager.
    // New backups get the first extension (.bib), so that they can be opened in JabRef directly.
    // .bak was used before; it is kept so that backups written by older versions are still found.
    BACKUP("Backup", "bib", "bak"),

    // Used when writing the .bib file. See {@link org.jabref.logic.exporter.AtomicFileWriter}
    // Used for copying the .bib away before overwriting on save.
    SAVE("AutoSaveFile", "sav");

    private final String name;

    private final List<String> extensions;

    BackupFileType(String name, String... extensions) {
        this.name = name;
        this.extensions = List.of(extensions);
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
