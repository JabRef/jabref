package net.sf.jabref.export;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.filechooser.FileFilter;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.MetaData;
import net.sf.jabref.Globals;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;

/**
 * Base class for export formats based on templates.
 * 
 */
public class ExportFormat implements IExportFormat {

	String displayName;
	String consoleName;
	String lfFileName;
	String directory;
	String extension;
	FileFilter fileFilter;
	boolean customExport = false;

	/**
	 * Initialize another export format based on templates stored in dir with
	 * layoutFile lfFilename.
	 * 
	 * @param displayName
	 *            Name to display to the user.
	 * @param consoleName
	 *            Name to call this format in the console.
	 * @param lfFileName
	 *            Name of the main layout file.
	 * @param directory
	 *            Directory in which to find the layout file.
	 * @param extension
	 *            Should contain the . (for instance .txt).
	 */
	public ExportFormat(String displayName, String consoleName,
		String lfFileName, String directory, String extension) {
		this.displayName = displayName;
		this.consoleName = consoleName;
		this.lfFileName = lfFileName;
		this.directory = directory;
		this.extension = extension;
	}

	/** Empty default constructor for subclasses */
	protected ExportFormat() {
		// intentionally empty
	}

	/**
	 * Indicate whether this is a custom export. A custom export looks for its
	 * layout files using a normal file path, while a built-in export looks in
	 * the classpath.
	 * 
	 * @param custom
	 *            true to indicate a custom export format.
	 */
	public void setCustomExport(boolean custom) {
		this.customExport = custom;
	}

	/**
	 * @see IExportFormat#getConsoleName()
	 */
	public String getConsoleName() {
		return consoleName;
	}

	/**
	 * @see IExportFormat#getDisplayName()
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * This method should return a reader from which the given layout file can
	 * be read.
	 * 
	 * This standard implementation of this method will use the
	 * {@link FileActions#getReader(String)} method.
	 * 
	 * Subclasses of ExportFormat are free to override and provide their own
	 * implementation.
	 * 
	 * @param filename
	 *            the file name
	 * @throws IOException
	 *             if the reader could not be created
	 * 
	 * @return a newly created reader
	 */
	protected Reader getReader(String filename) throws IOException {
		// If this is a custom export, just use the given file name:
		String dir;
		if (customExport) {
			dir = "";
		} else {
			dir = Globals.LAYOUT_PREFIX
				+ (directory == null ? "" : directory + "/");
		}
		return FileActions.getReader(dir + filename);
	}

	/**
	 * Perform the export of {@code database}.
	 * 
	 * @param database
	 *            The database to export from.
     * @param metaData
     *            The database's meta data.
	 * @param file
	 *            the file to write the resulting export to
	 * @param encoding
	 *            The encoding of the database
	 * @param entryIds
	 *            Contains the IDs of all entries that should be exported. If
	 *            <code>null</code>, all entries will be exported.
	 * 
	 * @throws IOException
	 *             if a problem occurred while trying to write to {@code writer}
	 *             or read from required resources.
	 * @throws Exception
	 *             if any other error occurred during export.
	 * 
	 * @see net.sf.jabref.export.IExportFormat#performExport(net.sf.jabref.BibtexDatabase,
	 *      net.sf.jabref.MetaData, java.lang.String, java.lang.String, java.util.Set)
	 */
	public void performExport(final BibtexDatabase database,
            final MetaData metaData, final String file,
		final String encoding, Set<String> entryIds) throws Exception {

		File outFile = new File(file);
		SaveSession ss = getSaveSession(encoding, outFile);
		VerifyingWriter ps = ss.getWriter();

		// Print header
		Layout beginLayout = null;
		Reader reader;
		try {
			reader = getReader(lfFileName + ".begin.layout");
			LayoutHelper layoutHelper = new LayoutHelper(reader);
			beginLayout = layoutHelper
				.getLayoutFromText(Globals.FORMATTER_PACKAGE);
			reader.close();
		} catch (IOException ex) {
			// If an exception was cast, export filter doesn't have a begin
			// file.
		}
		// Write the header
		if (beginLayout != null) {
			ps.write(beginLayout.doLayout(database, encoding));
		}

		/*
		 * Write database entries; entries will be sorted as they appear on the
		 * screen, or sorted by author, depending on Preferences. We also supply
		 * the Set entries - if we are to export only certain entries, it will
		 * be non-null, and be used to choose entries. Otherwise, it will be
		 * null, and be ignored.
		 */
		List<BibtexEntry> sorted = FileActions.getSortedEntries(database,
			entryIds, false);

		// Load default layout
		reader = getReader(lfFileName + ".layout");

		LayoutHelper layoutHelper = new LayoutHelper(reader);
		Layout defLayout = layoutHelper
			.getLayoutFromText(Globals.FORMATTER_PACKAGE);
		reader.close();
		HashMap<String, Layout> layouts = new HashMap<String, Layout>();
		Layout layout;
		for (BibtexEntry entry : sorted) {
			// Get the layout
			String type = entry.getType().getName().toLowerCase();
			if (layouts.containsKey(type))
				layout = layouts.get(type);
			else {
				try {
					// We try to get a type-specific layout for this entry.
					reader = getReader(lfFileName + "." + type + ".layout");
					layoutHelper = new LayoutHelper(reader);
					layout = layoutHelper
						.getLayoutFromText(Globals.FORMATTER_PACKAGE);
					layouts.put(type, layout);
					reader.close();
				} catch (IOException ex) {
					// The exception indicates that no type-specific layout
					// exists, so we
					// go with the default one.
					layout = defLayout;
				}
			}

			// Write the entry
			ps.write(layout.doLayout(entry, database));
		}

		// Print footer

		// changed section - begin (arudert)
		Layout endLayout = null;
		try {
			reader = getReader(lfFileName + ".end.layout");
			layoutHelper = new LayoutHelper(reader);
			endLayout = layoutHelper
				.getLayoutFromText(Globals.FORMATTER_PACKAGE);
			reader.close();
		} catch (IOException ex) {
			// If an exception was thrown, export filter doesn't have an end
			// file.
		}

		// Write footer
		if (endLayout != null) {
			ps.write(endLayout.doLayout(database, encoding));
		}

		finalizeSaveSession(ss);
	}

	protected SaveSession getSaveSession(final String encoding,
		final File outFile) throws IOException {
		return new SaveSession(outFile, encoding, false);
	}

	/**
	 * @see net.sf.jabref.export.IExportFormat#getFileFilter()
	 */
	public FileFilter getFileFilter() {
		if (fileFilter == null)
			fileFilter = new ExportFileFilter(this, extension);
		return fileFilter;
	}

	public void finalizeSaveSession(final SaveSession ss) throws Exception {
		ss.getWriter().flush();
		ss.getWriter().close();

		if (!ss.getWriter().couldEncodeAll()) {
			System.err.println("Could not encode...");
		}
		ss.commit();
	}
}
