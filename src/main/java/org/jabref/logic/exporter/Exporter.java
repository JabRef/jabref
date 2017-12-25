package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public interface Exporter {

    /**
     * Returns a one-word ID (used, for example, to identify the exporter in the console).
     */
    String getId();

    /**
     * Returns the name of the exporter (to display to the user).
     */
    String getDisplayName();

    /**
     * Returns the type of files this exporter creates.
     */
    FileType getFileType();

    /**
     * Performs the export.
     *
     * @param databaseContext the database to export from
     * @param file            the file to write to
     * @param encoding        the encoding to use
     * @param entries         a list containing all entries that should be exported
     */
    void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception;

    default String getDescription() {
        return getFileType().getDescription();
    }
}
