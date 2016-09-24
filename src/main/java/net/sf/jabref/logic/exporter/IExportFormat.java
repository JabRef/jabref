package net.sf.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

public interface IExportFormat {

    /**
     * Name to call this format in the console.
     */
    String getConsoleName();

    /**
     * Name to display to the user (for instance in the Save file format drop
     * down box.
     */
    String getDisplayName();

    String getExtension();

    /**
     * Perform the export.
     *
     * @param databaseContext the database to export from.
     * @param file
     *            The filename to write to.
     * @param encoding
     *            The encoding to use.
     * @param entries
     *            (may be null) A list containing all entries that
     *            should be exported. If null, all entries will be exported.
     * @throws Exception
     */
    void performExport(BibDatabaseContext databaseContext, String file, Charset encoding, List<BibEntry> entries)
            throws Exception;

    /**
     * Perform the Export.
     * Gets the path as a java.nio.path instead of a string.
     *
     * @param databaseContext the database to export from.
     * @param file  the Path to the file to write to.The path should be an java.nio.Path
     * @param encoding  The encoding to use.
     * @param entries (may be null) A list containing all entries that
     * should be exported. If null, all entries will be exported.
     * @throws Exception
     */
    void performExport(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries)
            throws Exception;

}
