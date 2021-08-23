package org.jabref.logic.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for export formats based on templates.
 */
public class TemplateExporter extends Exporter {

    private static final String BLANK_LINE_PATTERN = "\\r\\n|\\n";
    private static final String LAYOUT_PREFIX = "/resource/layout/";
    private static final String LAYOUT_EXTENSION = ".layout";
    private static final String FORMATTERS_EXTENSION = ".formatters";
    private static final String BEGIN_INFIX = ".begin";
    private static final String END_INFIX = ".end";

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateExporter.class);

    private final String lfFileName;
    private final String directory;
    private final LayoutFormatterPreferences layoutPreferences;
    private final SavePreferences savePreferences;
    private Charset encodingOverwritten; // If this value is set, it will be used to override the default encoding for the getCurrentBasePanel.
    private boolean customExport;
    private BlankLineBehaviour blankLineBehaviour;

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     * @param displayName Name to display to the user.
     * @param consoleName Name to call this format in the console.
     * @param lfFileName  Name of the main layout file.
     * @param directory   Directory in which to find the layout file.
     * @param extension   Should contain the . (for instance .txt).
     */
    public TemplateExporter(String displayName, String consoleName, String lfFileName, String directory, FileType extension) {
        this(displayName, consoleName, lfFileName, directory, extension, null, null);
    }

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     * @param name              to display to the user and to call this format in the console.
     * @param lfFileName        Name of the main layout file.
     * @param extension         May or may not contain the . (for instance .txt).
     * @param layoutPreferences Preferences for the layout
     * @param savePreferences   Preferences for saving
     */
    public TemplateExporter(String name, String lfFileName, String extension, LayoutFormatterPreferences layoutPreferences,
                            SavePreferences savePreferences) {
        this(name, name, lfFileName, null, StandardFileType.fromExtensions(extension), layoutPreferences, savePreferences);
    }

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     * @param displayName       Name to display to the user.
     * @param consoleName       Name to call this format in the console.
     * @param lfFileName        Name of the main layout file.
     * @param directory         Directory in which to find the layout file.
     * @param extension         Should contain the . (for instance .txt).
     * @param layoutPreferences Preferences for layout
     * @param savePreferences   Preferences for saving
     */
    public TemplateExporter(String displayName, String consoleName, String lfFileName, String directory, FileType extension,
                            LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {
        super(consoleName, displayName, extension);
        if (Objects.requireNonNull(lfFileName).endsWith(LAYOUT_EXTENSION)) {
            this.lfFileName = lfFileName.substring(0, lfFileName.length() - LAYOUT_EXTENSION.length());
        } else {
            this.lfFileName = lfFileName;
        }
        this.directory = directory;
        this.layoutPreferences = layoutPreferences;
        this.savePreferences = savePreferences;
    }

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     * @param displayName       Name to display to the user.
     * @param consoleName       Name to call this format in the console.
     * @param lfFileName        Name of the main layout file.
     * @param directory         Directory in which to find the layout file.
     * @param extension         Should contain the . (for instance .txt).
     * @param layoutPreferences Preferences for layout
     * @param savePreferences   Preferences for saving
     * @param blankLineBehaviour how to behave regarding blank lines.
     */
    public TemplateExporter(String displayName, String consoleName, String lfFileName, String directory, FileType extension,
                            LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences,
                            BlankLineBehaviour blankLineBehaviour) {
        super(consoleName, displayName, extension);
        if (Objects.requireNonNull(lfFileName).endsWith(LAYOUT_EXTENSION)) {
            this.lfFileName = lfFileName.substring(0, lfFileName.length() - LAYOUT_EXTENSION.length());
        } else {
            this.lfFileName = lfFileName;
        }
        this.directory = directory;
        this.layoutPreferences = layoutPreferences;
        this.savePreferences = savePreferences;
        this.blankLineBehaviour = blankLineBehaviour;
    }

    /**
     * Indicate whether this is a custom export.
     * A custom export looks for its layout files using a normal file path,
     * while a built-in export looks in the classpath.
     *
     * @param custom true to indicate a custom export format.
     */
    public void setCustomExport(boolean custom) {
        this.customExport = custom;
    }

    /**
     * Set an encoding which will be used in preference to the default value obtained from the basepanel.
     *
     * @param encoding The name of the encoding to use.
     */
    public TemplateExporter withEncoding(Charset encoding) {
        this.encodingOverwritten = encoding;
        return this;
    }

    /**
     * This method should return a reader from which the given layout file can be read.
     * <p>
     * Subclasses of TemplateExporter are free to override and provide their own implementation.
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
        URL reso = TemplateExporter.class.getResource(name);

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

    @Override
    public void export(final BibDatabaseContext databaseContext, final Path file,
                       final Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);

        Charset encodingToUse = StandardCharsets.UTF_8;
        if (encoding != null) {
            encodingToUse = encoding;
        } else if (this.encodingOverwritten != null) {
            encodingToUse = this.encodingOverwritten;
        }

        if (entries.isEmpty()) { // Do not export if no entries to export -- avoids exports with only template text
            return;
        }

        try (AtomicFileWriter ps = new AtomicFileWriter(file, encodingToUse)) {
            Layout beginLayout = null;

            // Check if this export filter has bundled name formatters:
            // Add these to the preferences, so all layouts have access to the custom name formatters:
            readFormatterFile();

            List<String> missingFormatters = new ArrayList<>(1);

            // Print header
            try (Reader reader = getReader(lfFileName + BEGIN_INFIX + LAYOUT_EXTENSION)) {
                LayoutHelper layoutHelper = new LayoutHelper(reader, layoutPreferences);
                beginLayout = layoutHelper.getLayoutFromText();
            } catch (IOException ex) {
                // If an exception was cast, export filter doesn't have a begin
                // file.
            }
            // Write the header
            if (beginLayout != null) {
                ps.write(beginLayout.doLayout(databaseContext, encodingToUse));
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
            try (Reader reader = getReader(lfFileName + LAYOUT_EXTENSION)) {
                layoutHelper = new LayoutHelper(reader, layoutPreferences);
                defLayout = layoutHelper.getLayoutFromText();
            }
            if (defLayout != null) {
                missingFormatters.addAll(defLayout.getMissingFormatters());
                if (!missingFormatters.isEmpty()) {
                    LOGGER.warn("Missing formatters found: {}", missingFormatters);
                }
            }
            Map<EntryType, Layout> layouts = new HashMap<>();
            Layout layout;

            ExporterFactory.entryNumber = 0;
            for (BibEntry entry : sorted) {
                ExporterFactory.entryNumber++; // Increment entry counter.
                // Get the layout
                EntryType type = entry.getType();
                if (layouts.containsKey(type)) {
                    layout = layouts.get(type);
                } else {
                    try (Reader reader = getReader(lfFileName + '.' + type.getName() + LAYOUT_EXTENSION)) {
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
                    if (blankLineBehaviour == BlankLineBehaviour.DELETE_BLANKS) {
                        String[] lines = layout.doLayout(entry, databaseContext.getDatabase()).split(BLANK_LINE_PATTERN);
                        for (String line : lines) {
                            if (!line.isBlank() && !line.isEmpty()) {
                                ps.write(line + OS.NEWLINE);
                            }
                        }
                    } else {
                        ps.write(layout.doLayout(entry, databaseContext.getDatabase()));
                    }
                }
            }

            // Print footer

            // changed section - begin (arudert)
            Layout endLayout = null;
            try (Reader reader = getReader(lfFileName + END_INFIX + LAYOUT_EXTENSION)) {
                layoutHelper = new LayoutHelper(reader, layoutPreferences);
                endLayout = layoutHelper.getLayoutFromText();
            } catch (IOException ex) {
                // If an exception was thrown, export filter doesn't have an end
                // file.
            }

            // Write footer
            if (endLayout != null) {
                ps.write(endLayout.doLayout(databaseContext, encodingToUse));
                missingFormatters.addAll(endLayout.getMissingFormatters());
            }

            // Clear custom name formatters:
            layoutPreferences.clearCustomExportNameFormatters();

            if (!missingFormatters.isEmpty() && LOGGER.isWarnEnabled()) {
                StringBuilder sb = new StringBuilder("The following formatters could not be found: ");
                sb.append(String.join(", ", missingFormatters));
                LOGGER.warn("Formatters {} not found", sb.toString());
            }
        }
    }

    /**
     * See if there is a name formatter file bundled with this export format.
     * If so, read all the name formatters so they can be used by the filter layouts.
     */
    private void readFormatterFile() {
        File formatterFile = new File(lfFileName + FORMATTERS_EXTENSION);
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

    public String getLayoutFileName() {
        return lfFileName;
    }

    public String getLayoutFileNameWithExtension() {
        return lfFileName + LAYOUT_EXTENSION;
    }
}
