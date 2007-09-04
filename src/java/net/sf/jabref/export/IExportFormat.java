package net.sf.jabref.export;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.MetaData;

import java.io.Writer;
import java.util.Set;

import javax.swing.filechooser.FileFilter;

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

	/**
	 * A file filter that accepts filetypes that this exporter would create.
	 */
	FileFilter getFileFilter();

	/**
	 * Perform the export.
	 * 
	 * @param database
	 *            The database to export from.
     * @param metaData
     *            The database's metadata.
	 * @param file
	 *            The filename to write to.
	 * @param encoding
	 *            The encoding to use.
	 * @param entryIds
	 *            (may be null) A Set containing the IDs of all entries that
	 *            should be exported. If null, all entries will be exported.
	 * @throws Exception
	 * @see #performExport(BibtexDatabase, Set, Writer)
	 */
	void performExport(BibtexDatabase database, MetaData metaData,
                       String file, String encoding,
		Set<String> entryIds) throws Exception;

}