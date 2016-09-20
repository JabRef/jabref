package net.sf.jabref.logic.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.jabref.JabRefMain;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for export formats based on templates.
 */
public class ExportFormat implements IExportFormat {

    private String displayName;
    private String consoleName;
    private String lfFileName;
    private String directory;
    private String extension;
    private Charset encoding; // If this value is set, it will be used to override
    // the default encoding for the getCurrentBasePanel.
    private LayoutFormatterPreferences layoutPreferences;
    private SavePreferences savePreferences;

    private boolean customExport;
    private static final String LAYOUT_PREFIX = "/resource/layout/";

    private static final Log LOGGER = LogFactory.getLog(ExportFormat.class);

    /**
     * Initialize another export format based on templates stored in dir with
     * layoutFile lfFilename.
     *
     * @param displayName Name to display to the user.
     * @param consoleName Name to call this format in the console.
     * @param lfFileName  Name of the main layout file.
     * @param directory   Directory in which to find the layout file.
     * @param extension   Should contain the . (for instance .txt).
     */
    public ExportFormat(String displayName, String consoleName, String lfFileName, String directory, String extension) {
        this.displayName = displayName;
        this.consoleName = consoleName;
        this.lfFileName = lfFileName;
        this.directory = directory;
        this.extension = extension;
    }

    /**
     * Initialize another export format based on templates stored in dir with
     * layoutFile lfFilename.
     *
     * @param displayName Name to display to the user.
     * @param consoleName Name to call this format in the console.
     * @param lfFileName  Name of the main layout file.
     * @param directory   Directory in which to find the layout file.
     * @param extension   Should contain the . (for instance .txt).
     * @param layoutPreferences Preferences for layout
     * @param savePreferences Preferences for saving
     */
    public ExportFormat(String displayName, String consoleName, String lfFileName, String directory, String extension,
            LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {
        this(displayName, consoleName, lfFileName, directory, extension);
        this.layoutPreferences = layoutPreferences;
        this.savePreferences = savePreferences;
    }

    /**
     * Empty default constructor for subclasses
     */
    protected ExportFormat() {
        // intentionally empty
    }

    /**
     * Indicate whether this is a custom export. A custom export looks for its
     * layout files using a normal file path, while a built-in export looks in
     * the classpath.
     *
     * @param custom true to indicate a custom export format.
     */
    public void setCustomExport(boolean custom) {
        this.customExport = custom;
    }

    /**
     * @see IExportFormat#getConsoleName()
     */
    @Override
    public String getConsoleName() {
        return consoleName;
    }

    /**
     * @see IExportFormat#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set an encoding which will be used in preference to the default value
     * obtained from the basepanel.
     *
     * @param encoding The name of the encoding to use.
     */
    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    /**
     * @see IExportFormat#getExtension()
     */
    @Override
    public String getExtension() {
        return extension;
    }

    /**
     * This method should return a reader from which the given layout file can
     * be read.
     * <p>
     * <p>
     * Subclasses of ExportFormat are free to override and provide their own
     * implementation.
     *
     * @param filename the filename
     * @return a newly created reader
     * @throws IOException if the reader could not be created
     */
    private Reader getReader(String filename) throws IOException {
        // If this is a custom export, just use the given filename:
        String dir;
        if (customExport) {
            dir = "";
        } else {
            dir = LAYOUT_PREFIX + (directory == null ? "" : directory + '/');
        }

        // Attempt to get a Reader for the file path given, either by
        // loading it as a resource (from within JAR), or as a normal file. If
        // unsuccessful (e.g. file not found), an IOException is thrown.
        String name = dir + filename;
        Reader reader;
        // Try loading as a resource first. This works for files inside the JAR:
        URL reso = JabRefMain.class.getResource(name);

        // If that did not work, try loading as a normal file URL:
        try {
            if (reso == null) {
                File f = new File(name);
                reader = new FileReader(f);
            } else {
                reader = new InputStreamReader(reso.openStream());
            }
        } catch (FileNotFoundException ex) {
            throw new IOException("Cannot find layout file: '" + name + "'.");
        }

        return reader;
    }

    /**
     * Perform the export of {@code database}.
     *
     * @param databaseContext the database to export from.
     * @param file       the file to write the resulting export to
     * @param encoding   The encoding of the database
     * @param entries    Contains all entries that should be exported.
     * @throws IOException if a problem occurred while trying to write to {@code writer}
     *                     or read from required resources.
     * @throws Exception   if any other error occurred during export.
     * @see net.sf.jabref.logic.exporter.IExportFormat#performExport(BibDatabaseContext, String, Charset, List)
     */
    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String file,
            final Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Do not export if no entries to export -- avoids exports with only template text
            return;
        }
        Path outFile = Paths.get(file);
        SaveSession ss = null;
        if (this.encoding != null) {
            try {
                ss = new FileSaveSession(this.encoding, false);
            } catch (SaveException ex) {
                // Perhaps the overriding encoding doesn't work?
                // We will fall back on the default encoding.
                LOGGER.warn("Can not get save session.", ex);
            }
        }
        if (ss == null) {
            ss = new FileSaveSession(encoding, false);
        }

        try (VerifyingWriter ps = ss.getWriter()) {

            Layout beginLayout = null;

            // Check if this export filter has bundled name formatters:
            // Add these to the preferences, so all layouts have access to the custom name formatters:
            readFormatterFile();

            List<String> missingFormatters = new ArrayList<>(1);

            // Print header
            try (Reader reader = getReader(lfFileName + ".begin.layout")) {
                LayoutHelper layoutHelper = new LayoutHelper(reader, layoutPreferences);
                beginLayout = layoutHelper.getLayoutFromText();
            } catch (IOException ex) {
                // If an exception was cast, export filter doesn't have a begin
                // file.
            }
            // Write the header
            if (beginLayout != null) {
                ps.write(beginLayout.doLayout(databaseContext, encoding));
                missingFormatters.addAll(beginLayout.getMissingFormatters());
            }

            /*
             * Write database entries; entries will be sorted as they appear on the
             * screen, or sorted by author, depending on Preferences. We also supply
             * the Set entries - if we are to export only certain entries, it will
             * be non-null, and be used to choose entries. Otherwise, it will be
             * null, and be ignored.
             */
            List<BibEntry> sorted = BibDatabaseWriter.getSortedEntries(databaseContext, entries, savePreferences);

            // Load default layout
            Layout defLayout;
            LayoutHelper layoutHelper;
            try (Reader reader = getReader(lfFileName + ".layout")) {
                layoutHelper = new LayoutHelper(reader,layoutPreferences);
                defLayout = layoutHelper.getLayoutFromText();
            }
            if (defLayout != null) {
                missingFormatters.addAll(defLayout.getMissingFormatters());
                if (!missingFormatters.isEmpty()) {
                    LOGGER.warn(missingFormatters);
                }
            }
            Map<String, Layout> layouts = new HashMap<>();
            Layout layout;

            ExportFormats.entryNumber = 0;
            for (BibEntry entry : sorted) {
                ExportFormats.entryNumber++; // Increment entry counter.
                // Get the layout
                String type = entry.getType();
                if (layouts.containsKey(type)) {
                    layout = layouts.get(type);
                } else {
                    try (Reader reader = getReader(lfFileName + '.' + type + ".layout")) {
                        // We try to get a type-specific layout for this entry.
                        layoutHelper = new LayoutHelper(reader, layoutPreferences);
                        layout = layoutHelper.getLayoutFromText();
                        layouts.put(type, layout);
                        if (layout != null) {
                            missingFormatters.addAll(layout.getMissingFormatters());
                        }

                    } catch (IOException ex) {
                        // The exception indicates that no type-specific layout
                        // exists, so we
                        // go with the default one.
                        layout = defLayout;
                    }
                }

                // Write the entry
                if (layout != null) {
                    ps.write(layout.doLayout(entry, databaseContext.getDatabase()));
                }
            }

            // Print footer

            // changed section - begin (arudert)
            Layout endLayout = null;
            try (Reader reader = getReader(lfFileName + ".end.layout")) {
                layoutHelper = new LayoutHelper(reader, layoutPreferences);
                endLayout = layoutHelper.getLayoutFromText();
            } catch (IOException ex) {
                // If an exception was thrown, export filter doesn't have an end
                // file.
            }

            // Write footer
            if (endLayout != null) {
                ps.write(endLayout.doLayout(databaseContext, this.encoding));
                missingFormatters.addAll(endLayout.getMissingFormatters());
            }

            // Clear custom name formatters:
            layoutPreferences.clearCustomExportNameFormatters();

            if (!missingFormatters.isEmpty()) {
                StringBuilder sb = new StringBuilder("The following formatters could not be found: ");
                sb.append(String.join(", ", missingFormatters));
                LOGGER.warn(sb);
            }
            finalizeSaveSession(ss, outFile);
        }

    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, Path file, final Charset encoding,
            List<BibEntry> entries) throws Exception {
        performExport(databaseContext, file.getFileName().toString(), encoding, entries);
    }

    /**
     * See if there is a name formatter file bundled with this export format. If so, read
     * all the name formatters so they can be used by the filter layouts.
     *
     */
    private void readFormatterFile() {
        File formatterFile = new File(lfFileName + ".formatters");
        if (formatterFile.exists()) {
            try (Reader in = new FileReader(formatterFile)) {
                // Ok, we found and opened the file. Read all contents:
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = in.read()) != -1) {
                    sb.append((char) c);
                }
                String[] lines = sb.toString().split("\n");
                // Go through each line:
                for (String line1 : lines) {
                    String line = line1.trim();
                    // Do not deal with empty lines:
                    if (line.isEmpty()) {
                        continue;
                    }
                    int index = line.indexOf(':'); // TODO: any need to accept escaped colons here?
                    if ((index > 0) && ((index + 1) < line.length())) {
                        String formatterName = line.substring(0, index);
                        String contents = line.substring(index + 1);
                        layoutPreferences.putCustomExportNameFormatter(formatterName, contents);
                    }
                }

            } catch (IOException ex) {
                // TODO: show error message here?
                LOGGER.warn("Problem opening formatter file.", ex);
            }
        }
    }

    public void finalizeSaveSession(final SaveSession ss, Path file) throws SaveException, IOException {
        ss.getWriter().flush();
        ss.getWriter().close();

        if (!ss.getWriter().couldEncodeAll()) {
            LOGGER.warn("Could not encode...");
        }
        ss.commit(file);
    }
}
