package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public abstract class Exporter {

    private final String id;
    private final String displayName;
    private final FileType fileType;

    public Exporter(String id, String displayName, FileType extension) {
        this.id = id;
        this.displayName = displayName;
        this.fileType = extension;
    }

    /**
     * Returns a one-word ID (used, for example, to identify the exporter in the console).
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the exporter (to display to the user).
     */
    public String getName() {
        return displayName;
    }

    /**
     * Returns the type of files this exporter creates.
     */
    public FileType getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Performs the export.
     *
     * @param databaseContext the database to export from
     * @param file            the file to write to
     * @param encoding        the encoding to use
     * @param entries         a list containing all entries that should be exported
     */
    public abstract void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception;
}
