package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

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
     *             A list containing all entries that
     *            should be exported. The list of entries must be non null
     * @throws Exception
     */
    void performExport(BibDatabaseContext databaseContext, String file, Charset encoding, List<BibEntry> entries)
            throws Exception;

}
