package net.sf.jabref.export;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

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
    String encoding = null; // If this value is set, it will be used to override
      // the default encoding for the basePanel.

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
     * Set an encoding which will be used in preference to the default value
     * obtained from the basepanel.
     * @param encoding The name of the encoding to use.
     */
    protected void setEncoding(String encoding) {
        this.encoding = encoding;
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
        SaveSession ss = null;
        if (this.encoding != null) {
            try {
                ss = getSaveSession(this.encoding, outFile);
            } catch (IOException ex) {
                // Perhaps the overriding encoding doesn't work?
                // We will fall back on the default encoding.
                ex.printStackTrace();

            }
        }
		if (ss == null)
		    ss = getSaveSession(encoding, outFile);
        
		VerifyingWriter ps = ss.getWriter();

        Layout beginLayout = null;
		Reader reader = null;

        // Check if this export filter has bundled name formatters:
        HashMap<String,String> customNameFormatters = readFormatterFile(lfFileName);
        // Set a global field, so all layouts have access to the custom name formatters: 
        Globals.prefs.customExportNameFormatters = customNameFormatters;

        ArrayList<String> missingFormatters = new ArrayList<String>(1);

        // Print header
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
            missingFormatters.addAll(beginLayout.getMissingFormatters());
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
        if (defLayout != null) {
            missingFormatters.addAll(defLayout.getMissingFormatters());
            System.out.println(defLayout.getMissingFormatters());
        }
		HashMap<String, Layout> layouts = new HashMap<String, Layout>();
		Layout layout;

        ExportFormats.entryNumber = 0;
		for (BibtexEntry entry : sorted) {
            ExportFormats.entryNumber++; // Increment entry counter.
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
                    if (layout != null)
                        missingFormatters.addAll(layout.getMissingFormatters());

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
            missingFormatters.addAll(endLayout.getMissingFormatters());
		}

        // Clear custom name formatters:
        Globals.prefs.customExportNameFormatters = null;

        if (missingFormatters.size() > 0) {
            StringBuilder sb = new StringBuilder("The following formatters could not be found").
                    append(": ");
            for (Iterator<String> i = missingFormatters.iterator(); i.hasNext();) {
                sb.append(i.next());
                if (i.hasNext())
                    sb.append(", ");
            }
            System.err.println(sb.toString());
        }

        finalizeSaveSession(ss);
	}

    /**
     * See if there is a name formatter file bundled with this export format. If so, read
     * all the name formatters so they can be used by the filter layouts.
     * @param lfFileName The layout file name.
     */
    private HashMap<String, String> readFormatterFile(String lfFileName) {
        HashMap<String,String> formatters = new HashMap<String, String>();
        File formatterFile = new File(lfFileName + ".formatters");
        if (formatterFile.exists()) {
            Reader in = null;
            try {
                in = new FileReader(formatterFile);
                if (in != null) {
                    // Ok, we found and opened the file. Read all contents:
                    StringBuilder sb = new StringBuilder();
                    int c;
                    while ((c = in.read()) != -1) {
                        sb.append((char)c);
                    }
                    String[] lines = sb.toString().split("\n");
                    // Go through each line:
                    for (int i=0; i<lines.length; i++) {
                        String line = lines[i].trim();
                        // Do not deal with empty lines:
                        if (line.length() == 0)
                            continue;
                        int index = line.indexOf(":"); // TODO: any need to accept escaped colons here?
                        if ((index > 0) && (index+1 < line.length())) {
                            String formatterName = line.substring(0, index);
                            String contents = line.substring(index+1);
                            //System.out.println("Name: '"+formatterName+"'");
                            //System.out.println("Contents: '"+contents+"'");
                            formatters.put(formatterName, contents);
                        }
                    }
                }

            } catch (IOException ex) {
                // TODO: show error message here?
                ex.printStackTrace();
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
            }
        }
        return formatters;
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
